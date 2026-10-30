BUILD AUTOBUILD BANG GITHUB ACTIONS

1. Dang nhap GitHub va tao repository moi (New repository).
2. Dat ten, vi du AutoBuild-Fabric-1.21.11, chon Public hoac Private, bam Create repository.
3. Giai nen thu muc AutoBuild_1.21.11_OneJava tren may.
4. Trong repository GitHub, chon Add file > Upload files.
5. Keo TOAN BO NOI DUNG ben trong thu muc AutoBuild_1.21.11_OneJava vao trang upload (bao gom thu muc .github; neu khong thay .github, bat hien thi hidden files hoac upload qua Git).
6. Bam Commit changes.
7. Mo tab Actions cua repository. Workflow 'Build AutoBuild JAR' se tu chay sau khi push.
8. Chon lan chay moi nhat co dau xanh (Success).
9. Keo xuong muc Artifacts, tai AutoBuild-Fabric-1.21.11.zip.
10. Giai nen artifact; file autobuild-0.1.0.jar la mod de bo vao .minecraft/mods.

Neu Actions chua chay: vao Actions > Build AutoBuild JAR > Run workflow.

Luu y:
- Workflow chi build ma nguon hien tai. Build thanh cong khong dong nghia mod da co chuc nang doc schematic/tu di chuyen/tu dat block.
- File JAR nay la ban giao dien/prototype hien tai.
- Khong upload token, mat khau hay khoa rieng len repository.
