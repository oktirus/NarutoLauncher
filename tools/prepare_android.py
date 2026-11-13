"""Create a separate offline Android shell from the preserved Yoru reference."""
from pathlib import Path
import shutil
ROOT=Path(__file__).resolve().parents[1];REF=ROOT.parent/'AnimeLauncher/android';OUT=ROOT/'android'
for file in ['build.gradle','settings.gradle','gradle.properties','gradlew','gradlew.bat','gradle/wrapper/gradle-wrapper.jar','gradle/wrapper/gradle-wrapper.properties']:
 dst=OUT/file;dst.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(REF/file,dst)
(OUT/'settings.gradle').write_text((REF/'settings.gradle').read_text().replace("rootProject.name = 'Yoru'","rootProject.name = 'Konoha'"),encoding='utf8')
src=REF/'app/src/main/java/ua/yoru/tv/MainActivity.java'
java=src.read_text(encoding='utf8').replace('package ua.yoru.tv;','package ua.konoha.tv;').replace('https://yoru.local','https://konoha.local').replace('"yoru.local"','"konoha.local"')
java=java.replace('web = new WebView(this);','web = new WebView(this);\n        WebView.setWebContentsDebuggingEnabled((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0);')
java=java.replace('String file = "ui" + ("/".equals(path) ? "/index.html" : path);','if (!path.startsWith("/prototype/") && !path.startsWith("/assets/")) return missing();\n                    String file = "site" + path;')
start=java.index('String mime = path.endsWith');end=java.index('return new WebResourceResponse(mime',start)
java=java[:start]+'''String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(path);
                    String mime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                    if ("js".equals(extension)) mime = "text/javascript";
                    if (mime == null) mime = "application/octet-stream";
                    '''+java[end:]
java=java.replace('ORIGIN + "/index.html"','ORIGIN + "/prototype/index.html"')
dst=OUT/'app/src/main/java/ua/konoha/tv/MainActivity.java';dst.parent.mkdir(parents=True,exist_ok=True);dst.write_text(java,encoding='utf8')
print('Independent ua.konoha.tv shell prepared; Yoru source unchanged')
