AUTOBUILD - FABRIC 1.21.11 - SOURCE 1 FILE JAVA
================================================

CANH BAO QUAN TRONG
- Day la bo khung giao dien/menu, chua doc noi dung .litematic va CHUA tu dat block,
  tu lay vat lieu, tim duong hay dieu khien nhan vat.
- Nut LOAD FILE hien mo hop thoai chon file va hien duong dan; chua phan tich schematic.
- Khong the chi doi ten .java thanh .jar. Phai bien dich bang Gradle/Loom.

CAU TRUC
AutoBuild/
  build.gradle
  gradle.properties
  settings.gradle
  src/main/java/vn/autobuild/AutoBuildClient.java
  src/main/resources/fabric.mod.json

YEU CAU
- JDK 21 (khong chi JRE).
- Gradle 8.x phu hop voi Fabric Loom 1.13.6; cach de nhat la dung IntelliJ IDEA va mo build.gradle.
- Ket noi Internet lan dau de Gradle tai Minecraft, Yarn, Fabric Loader va Fabric API.

CACH BUILD TREN WINDOWS
1. Cai JDK 21, kiem tra:
   java -version
   javac -version
2. Cai Gradle, kiem tra:
   gradle --version
3. Giai nen thu muc AutoBuild.
4. Mo CMD/PowerShell tai thu muc co build.gradle.
5. Chay:
   gradle build
6. Neu build thanh cong, file mod da remap nam tai:
   build\libs\autobuild-0.1.0.jar
   (Khong chon file co hau to -dev.)

TAO GRADLE WRAPPER (khong bat buoc)
Neu may da cai Gradle, chay mot lan:
   gradle wrapper --gradle-version 8.14.3
Sau do build bang:
   .\gradlew.bat build
Wrapper se tao cac file gradlew, gradlew.bat va gradle/wrapper.

CAI VAO MINECRAFT
1. Dung Minecraft Java 1.21.11 voi Fabric Loader.
2. Cai Fabric API phien ban danh cho 1.21.11 vao .minecraft/mods.
3. Chep autobuild-0.1.0.jar vao .minecraft/mods.
4. Mo game bang profile Fabric 1.21.11.
5. Trong game nhan B de mo menu.

NEU BUILD LOI
- Kiem tra java -version phai la 21.
- Kiem tra ban Minecraft dung 1.21.11.
- Trong IntelliJ: mo build.gradle, chon Load/Reload Gradle Project.
- Chay: gradle build --refresh-dependencies
- Gui nguyen van loi trong cua so Build de chan doan; khong xoa cac dong loi dau tien.

LUU Y VE SCHEMATIC
File .litematic khong can giai nen. Ban prototype nay chi cho chon file va dien duong dan.
Can phat trien them bo doc dinh dang Litematica va logic dat block truoc khi nut Preview/Start
co the xay that. Viec tu di chuyen/tu dat block con phu thuoc server, quyen va co che game.
