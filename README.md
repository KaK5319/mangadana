# 漫画棚（mangadana）

漫画本棚アプリ。旧リポジトリ sturdy-spoon（SideBooks）からアプリ本体を引き継ぎ、
ビルド構成を映画棚（movie-shelf）方式に統一した新規リポジトリ。

- アプリ本体: lib/main.dart（単一ファイル）＋ shaders/page_curl.frag（ページカール）
- 表示名「漫画棚」は AndroidManifest.xml に最初から設定（ビルド時の書き換えなし）
- applicationId: com.example.mangadana（旧アプリとは別アプリとしてインストールされる）
- アイコン: icon/icons.b64（PNG一式を base64 化したもの）を Codemagic ビルド時に展開
- ビルド: Codemagic（codemagic.yaml）。完成APKの名前とアイコンはビルド後検証ステップで確認する
