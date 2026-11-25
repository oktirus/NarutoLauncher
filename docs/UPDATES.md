# Оновлення Конохи

Установлена нативна версія перевіряє стабільні випуски [oktirus/NarutoLauncher](https://github.com/oktirus/NarutoLauncher/releases/latest). У **Шлях ніндзя → Про Коноху та ресурси → Оновлення Конохи** можна перевірити їх вручну. Автоматична перевірка запускається після відкриття лаунчера. Після успішної перевірки повторюється приблизно за 30 хвилин, після помилки — за 5 хвилин, якщо застосунок і далі відкритий. Доступна версія з’являється біля годинника. Якщо в цей момент відкрите меню чи інша панель, пропозиція завантаження чекає на повернення до вільного головного екрана.

За наявності більшого versionCode лаунчер пропонує завантаження, перевіряє розмір/SHA-256, package ID та сертифікат підпису, потім показує кнопку «Встановити». Першого разу Android може попросити дозвіл встановлювати програми з Конохи. Після повернення потрібно ще раз підтвердити встановлення. Остаточне рішення приймається в системному вікні Android. Відмова залишає попередню версію і дані; HOME updater не змінює.

Версії до 1.1.0 ще не містять цього механізму: перший перехід на 1.1.0 потребує звичайного встановлення APK поверх старого з тим самим підписом. Ключ не передається до GitHub. CI-артефакти підписані окремим тимчасовим debug key runner і **не є каналом сумісних оновлень**; для встановлення бери APK з GitHub Releases.

## Публікація наступного випуску

1. Збільшити versionCode і versionName у `native-parity/app/build.gradle`; не використовувати той самий versionCode для іншого APK.
2. Зібрати локально тим самим ключем: `pwsh -NoProfile -File native-parity/Build.ps1`. Пройти тести/lint та оглянути змінений UI.
3. Підготувати UTF-8 release notes. Згенерувати й перевірити staging без публікації:

```powershell
python -X utf8 tools/publish_release.py --apk native-parity/releases/KonohaNative-1.1.0-debug.apk --notes-file docs/releases/1.1.0.md
```

4. Після явного доручення на Release додати `--publish`. Потрібні чистий committed working tree, успішний GitHub Android workflow для поточного commit і version metadata APK, що збігається з app/build.gradle. Скрипт звіряє сертифікат із `.release-certificate.sha256`, створює draft з APK/update.json, завантажує їх назад для перевірки й тільки потім публікує stable release. Наявний tag не перезаписується.

`update.json`: schemaVersion=1, applicationId, integer versionCode, versionName, apkName, bytes, sha256, notes. APK має бути asset того самого release. У APK немає GitHub token. Власний keystore зберігати окремо й резервувати безпечно; зміна ключа порушує update-сумісність.

Джерела: [GitHub Releases API](https://docs.github.com/en/rest/releases/releases#get-the-latest-release), [PackageInstaller](https://developer.android.com/reference/android/content/pm/PackageInstaller). OEM може обмежувати встановлення APK; це перевіряється на конкретному телевізорі.
