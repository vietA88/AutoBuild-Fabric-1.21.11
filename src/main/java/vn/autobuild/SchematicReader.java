package vn.autobuild;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Basic reader for gzip-compressed Sponge .schem and Litematica .litematic.
 * Returns local block coordinates and palette states. Placement/AI building
 * must be implemented separately in the Fabric client.
 */
public final class SchematicReader {
    private SchematicReader() {}

    public record Pos(int x, int y, int z) {}
    public record State(String id, Map<String, String> properties) {}
    public record Result(String name, int width, int height, int length,
                         Map<Integer, State> palette, Map<Pos, Integer> blocks) {}

    public static Result read(Path file) throws IOException {
        try (InputStream in = new GZIPInputStream(Files.newInputStream(file))) {
            Tag root = Nbt.root(in);
            Map<String, Tag> n = map(root.value);
            if (n.containsKey("Regions")) return litematic(n);
            if (n.containsKey("Schematic")) n = map(n.get("Schematic").value);
            if (n.containsKey("Palette") && n.containsKey("BlockData"))
                return sponge(n, file.getFileName().toString());
            throw new IOException("Unsupported NBT schematic format");
        }
    }

    private static Result sponge(Map<String, Tag> n, String fallback) throws IOException {
        int w = num(n, "Width", -1), h = num(n, "Height", -1), l = num(n, "Length", -1);
        check(w, h, l);
        Map<Integer, State> palette = new HashMap<>();
        for (var e : map(n.get("Palette").value).entrySet())
            palette.put(Integer.parseInt(e.getKey()), state(e.getValue().str()));
        Object raw = n.get("BlockData").value;
        if (!(raw instanceof byte[] data)) throw new IOException("Missing Sponge BlockData");
        long volume = (long) w * h * l;
        if (volume > 100_000_000) throw new IOException("Schematic too large");
        Map<Pos, Integer> blocks = new HashMap<>();
        int[] cursor = {0};
        for (int i = 0; i < (int) volume; i++) {
            int p = varint(data, cursor);
            if (!palette.containsKey(p)) throw new IOException("Unknown palette index " + p);
            if (p != 0) blocks.put(new Pos(i % w, i / (w * l), (i / w) % l), p);
        }
        return new Result(str(n, "Metadata.Name", fallback), w, h, l,
                Map.copyOf(palette), Map.copyOf(blocks));
    }

    private static Result litematic(Map<String, Tag> n) throws IOException {
        Map<String, Tag> regions = map(n.get("Regions").value);
        Map<Integer, State> palette = new HashMap<>();
        Map<Pos, Integer> blocks = new HashMap<>();
        int maxX = 0, maxY = 0, maxZ = 0, offset = 0;

        for (var re : regions.entrySet()) {
            Map<String, Tag> r = map(re.getValue().value);
            Map<String, Tag> size = map(r.get("Size").value);
            int sx = Math.abs(num(size, "x", 0)), sy = Math.abs(num(size, "y", 0)), sz = Math.abs(num(size, "z", 0));
            check(sx, sy, sz);
            maxX = Math.max(maxX, sx); maxY = Math.max(maxY, sy); maxZ = Math.max(maxZ, sz);
            Map<String, Tag> origin = map(r.get("Position").value);
            int ox = num(origin, "x", 0), oy = num(origin, "y", 0), oz = num(origin, "z", 0);

            List<Tag> states = (List<Tag>) r.get("BlockStatePalette").value;
            List<State> local = new ArrayList<>();
            for (Tag t : states) {
                Map<String, Tag> s = map(t.value);
                Map<String, String> props = new TreeMap<>();
                if (s.containsKey("Properties"))
                    for (var p : map(s.get("Properties").value).entrySet()) props.put(p.getKey(), p.getValue().str());
                local.add(new State(str(s, "Name", "minecraft:air"), Map.copyOf(props)));
            }
            for (int i = 0; i < local.size(); i++) palette.put(offset + i, local.get(i));

            long[] packed = (long[]) r.get("BlockStates").value;
            int bits = Math.max(2, 32 - Integer.numberOfLeadingZeros(Math.max(1, local.size() - 1)));
            int perLong = 64 / bits;
            long mask = (1L << bits) - 1;
            long volume = (long) sx * sy * sz;
            if (volume > 100_000_000) throw new IOException("Litematica region too large");
            for (int i = 0; i < (int) volume; i++) {
                int li = i / perLong, shift = (i % perLong) * bits;
                if (li >= packed.length) throw new IOException("Truncated BlockStates");
                int pi = (int) ((packed[li] >>> shift) & mask);
                if (pi >= local.size()) throw new IOException("Invalid palette index " + pi);
                if (!local.get(pi).id().equals("minecraft:air"))
                    blocks.put(new Pos(ox + i % sx, oy + i / (sx * sz), oz + (i / sx) % sz), offset + pi);
            }
            offset += local.size();
        }
        return new Result("Litematica", maxX, maxY, maxZ, Map.copyOf(palette), Map.copyOf(blocks));
    }

    private static State state(String text) {
        int i = text.indexOf('[');
        if (i < 0) return new State(text, Map.of());
        String id = text.substring(0, i), body = text.endsWith("]") ? text.substring(i + 1, text.length() - 1) : text.substring(i + 1);
        Map<String, String> props = new TreeMap<>();
        for (String part : body.split(",")) {
            int eq = part.indexOf('=');
            if (eq > 0) props.put(part.substring(0, eq).trim(), part.substring(eq + 1).trim());
        }
        return new State(id, Map.copyOf(props));
    }
    private static int varint(byte[] a, int[] at) throws IOException {
        int value = 0, shift = 0;
        while (true) {
            if (at[0] >= a.length || shift >= 35) throw new IOException("Invalid BlockData VarInt");
            int b = a[at[0]++] & 255;
            value |= (b & 127) << shift;
            if ((b & 128) == 0) return value;
            shift += 7;
        }
    }
    private static void check(int x, int y, int z) throws IOException {
        if (x <= 0 || y <= 0 || z <= 0 || (long)x*y*z > 100_000_000)
            throw new IOException("Invalid schematic dimensions " + x + "x" + y + "x" + z);
    }
    private static int num(Map<String, Tag> m, String k, int d) {
        Tag t = m.get(k); return t != null && t.value instanceof Number ? ((Number)t.value).intValue() : d;
    }
    private static String str(Map<String, Tag> m, String k, String d) {
        Tag t = m.get(k); return t == null ? d : t.str();
    }
    @SuppressWarnings("unchecked")
    private static Map<String, Tag> map(Object o) { return (Map<String, Tag>) o; }

    private record Tag(int type, Object value) {
        String str() { return value instanceof String ? (String)value : String.valueOf(value); }
    }

    private static final class Nbt {
        static Tag root(InputStream in) throws IOException {
            int type = u8(in);
            if (type != 10) throw new IOException("NBT root is not a compound");
            string(in);
            return new Tag(10, payload(in, type, 0));
        }
        static Object payload(InputStream in, int type, int depth) throws IOException {
            if (depth > 512) throw new IOException("NBT too deeply nested");
            return switch (type) {
                case 1 -> (byte)u8(in);
                case 2 -> (short)i16(in);
                case 3 -> i32(in);
                case 4 -> i64(in);
                case 5 -> Float.intBitsToFloat(i32(in));
                case 6 -> Double.longBitsToDouble(i64(in));
                case 7 -> { int n = length(i32(in)); byte[] a = new byte[n]; read(in,a); yield a; }
                case 8 -> string(in);
                case 9 -> {
                    int child = u8(in), n = length(i32(in)); List<Tag> a = new ArrayList<>(n);
                    for (int i=0;i<n;i++) a.add(new Tag(child,payload(in,child,depth+1)));
                    yield a;
                }
                case 10 -> {
                    Map<String,Tag> m = new LinkedHashMap<>();
                    while (true) { int child=u8(in); if(child==0) break; m.put(string(in),new Tag(child,payload(in,child,depth+1))); }
                    yield m;
                }
                case 11 -> { int n=length(i32(in)); int[] a=new int[n]; for(int i=0;i<n;i++)a[i]=i32(in); yield a; }
                case 12 -> { int n=length(i32(in)); long[] a=new long[n]; for(int i=0;i<n;i++)a[i]=i64(in); yield a; }
                default -> throw new IOException("Unknown NBT tag " + type);
            };
        }
        static int length(int n) throws IOException { if(n<0 || n>100_000_000) throw new IOException("Invalid NBT length"); return n; }
        static int u8(InputStream in) throws IOException { int b=in.read(); if(b<0)throw new EOFException(); return b; }
        static int i16(InputStream in) throws IOException { return (u8(in)<<8)|u8(in); }
        static int i32(InputStream in) throws IOException { return (u8(in)<<24)|(u8(in)<<16)|(u8(in)<<8)|u8(in); }
        static long i64(InputStream in) throws IOException { return ((long)i32(in)<<32)|(i32(in)&0xffffffffL); }
        static String string(InputStream in) throws IOException { int n=i16(in); byte[] b=new byte[n]; read(in,b); return new String(b,StandardCharsets.UTF_8); }
        static void read(InputStream in, byte[] b) throws IOException { int p=0; while(p<b.length){int n=in.read(b,p,b.length-p);if(n<0)throw new EOFException();p+=n;} }
    }
}
