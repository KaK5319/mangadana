package com.example.mangadana

import android.os.Handler
import android.os.Looper
import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread

class MainActivity : FlutterActivity() {
    private val rarChannelName = "com.example.mangadana/rar"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, rarChannelName)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "extractRar" -> {
                        val rarPath = call.argument<String>("rarPath")
                        val destDir = call.argument<String>("destDir")
                        if (rarPath == null || destDir == null) {
                            result.error("BAD_ARGS", "rarPath / destDir が必要です", null)
                        } else {
                            extractRar(rarPath, destDir, result)
                        }
                    }
                    else -> result.notImplemented()
                }
            }
    }

    /// RAR(RAR5含む)を Junrar(純Java)で展開する。大きなアーカイブでも
    /// メモリに抱え込まないよう、1エントリずつストリームでディスクへ書き出す。
    /// 重い処理なので別スレッドで実行し、結果はメインスレッドで返す。
    /// 戻り値は書き出したファイルの相対パス一覧。失敗は例外をエラーとして
    /// 返すので、Dart側では通常の失敗表示になる(アプリごと落ちたりしない)。
    private fun extractRar(rarPath: String, destDir: String, result: MethodChannel.Result) {
        val mainHandler = Handler(Looper.getMainLooper())
        thread(name = "rar-extract") {
            try {
                val dest = File(destDir)
                dest.mkdirs()
                val destRoot = dest.canonicalFile
                val extracted = mutableListOf<String>()
                Archive(File(rarPath)).use { archive ->
                    if (archive.isEncrypted) {
                        throw IllegalStateException("パスワードで暗号化されたRARは取り込めません")
                    }
                    var header: FileHeader? = archive.nextFileHeader()
                    while (header != null) {
                        val fh = header
                        if (!fh.isDirectory) {
                            if (fh.isEncrypted) {
                                throw IllegalStateException("パスワードで暗号化されたファイルが含まれているため取り込めません")
                            }
                            val rel = fh.fileName.replace('\\', '/').trimStart('/')
                            if (rel.isEmpty()) {
                                header = archive.nextFileHeader()
                                continue
                            }
                            // 変な名前でも行き先フォルダの外へは書かない
                            val out = File(destRoot, rel).canonicalFile
                            if (!out.path.startsWith(destRoot.path + File.separator)) {
                                throw IllegalStateException("不正なファイル名が含まれています: $rel")
                            }
                            out.parentFile?.mkdirs()
                            BufferedOutputStream(FileOutputStream(out)).use { os ->
                                archive.extractFile(fh, os)
                            }
                            extracted.add(rel)
                        }
                        header = archive.nextFileHeader()
                    }
                }
                mainHandler.post { result.success(extracted) }
            } catch (e: Exception) {
                mainHandler.post {
                    result.error("EXTRACT_FAILED", e.message ?: "RARを展開できませんでした", null)
                }
            }
        }
    }
}
