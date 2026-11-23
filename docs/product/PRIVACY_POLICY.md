# Privacy Policy — Midad Studio (استوديو مِداد)

**Policy version:** 4.0 · **Effective date:** 2026-11-23 · **Last updated:** 2026-11-23
**Applies to:** the Android application **Midad Studio**, package `com.midad.studio`, version **0.2.3**
(versionCode **5**) and any later version that links to this page.

**Where this policy lives.** The same text is published as a self-contained page at
[docs/privacy/index.html](../privacy/index.html) and at
https://ahmadmhmdsy.github.io/midad-studio/privacy/; the hosted copy is the version of record. The in-app
privacy summary (Settings → Privacy) is a short notice, not the policy. Privacy questions and deletion
requests can be sent through the project's [GitHub Issues](https://github.com/ahmadmhmdsy/midad-studio/issues).

---

## Who we are

- **Publisher / data controller:** Ahmad Mahmoud
- **Privacy contact:** the project's [GitHub Issues](https://github.com/ahmadmhmdsy/midad-studio/issues)
- **App:** Midad Studio for Android, package `com.midad.studio`.

"We" and "us" below mean that publisher. We operate no server, no account system and no way to identify you
from the app. There is no developer-side database about users of this app, because no user data is ever sent
to us.

## The short version

- Your photos and videos are edited **on your phone** and stay there. Nothing you edit is uploaded to us.
- There are **no accounts, no ads, no analytics, no crash-reporting SDK and no tracking** of any kind.
- The app declares **one permission: internet access**, used only when *you* ask it to download an optional AI
  model file.
- **Android backup is switched off** for the app, so the app's data is not copied to Google Drive and does not
  move to a new phone through the operating system.
- You can remove everything by deleting it inside the app or by uninstalling the app. Exports you saved to your
  gallery are your files and stay until you delete them.

### الخلاصة بالعربية

- تُعالَج صورك وفيديوهاتك **على هاتفك** وتبقى عليه. لا يُرفع إلينا شيء مما تحرّره.
- لا حساب، ولا إعلانات، ولا تحليلات، ولا تقارير أعطال، ولا تتبّع من أي نوع.
- الإذن الوحيد الذي يطلبه التطبيق هو **الإنترنت**، ولا يُستخدم إلا عندما تطلب أنت تنزيل ملف نموذج ذكاء اصطناعي
  اختياري.
- **النسخ الاحتياطي في أندرويد مُعطَّل** لهذا التطبيق، فلا تُنسخ بياناته إلى Google Drive ولا تنتقل إلى هاتف جديد
  عبر النظام.
- يمكنك حذف كل شيء من داخل التطبيق أو بإزالة التطبيق. أما الملفات التي صدّرتها إلى معرض الصور فهي ملفاتك وتبقى
  حتى تحذفها.

## 1. What the app processes, where it lives, and whether it leaves your device

| What | Where it is kept | Why | Leaves your device? |
|---|---|---|---|
| The photo or video you pick | Read into memory through the Android Photo Picker, for the session you are editing | So you can edit the item you chose | **No** |
| Your edit in progress (layers, adjustments, crop, text, drawing) | App memory while the studio is open; a design or video draft is not a cloud document and is not uploaded | So the editor and undo/redo work | **No** |
| The exported image or video | Your own gallery through Android's MediaStore; on Android 7.0–9.0 (API 26–28) a file in app-specific storage is handed to the app you share it with through a non-exported FileProvider | To give you the finished file | **No**, until you choose to share it |
| Project records: display name, size, date, and a small thumbnail of recent exports | The app's private storage; thumbnails live in a directory excluded from backup | So the Projects screen can list recent work | **No** |
| Settings: theme, language, last-used studio, model choices | The app's private storage | To remember your preferences | **No** |
| AI model files (bundled models copied out of the APK, or models you chose to download) and their SHA-256 checksums | The app's private files directory | To run the on-device cutout offline | **No** |
| The model download request itself: the file URL requested, your IP address, user agent, time and bytes transferred | The model host's own logs | To deliver the file you asked for | **Yes — to that host only**, as with any download |

We never receive, see or store any of the items in the first six rows. The app has no server to send them to.

## 2. What we never do

- No account, sign-in, profile, user identifier or device fingerprint.
- No advertising, ad SDK, advertising ID or attribution SDK.
- No analytics, telemetry, event logging, session recording, heat maps or A/B testing.
- No crash-reporting SDK: if the app crashes, the operating system may write a local log, but nothing is sent
  to us and we have no way to read it.
- No upload of your media, projects, exports, thumbnails or model choices to us or to any third party.
- No sale, rent or "sharing" of personal data as Google Play defines it.
- No profiling, no automated decision-making, and no training of AI models on your content.
- No access to your camera, microphone, location, contacts, calendar, SMS, call log or phone state.

## 3. Network use and recipients

The app makes **one kind of network request**, and only when you start it:

1. **An optional AI model download.** The AI Lab lists models from a catalogue shipped inside the app
   (`assets/models/manifest.json`). Models marked as bundled are already in the app and never need the network.
   A model marked for download is fetched only after you tap download. The request is a plain HTTPS
   `GET` for one file, issued by the app's own downloader
   (`core/ai/internal/DefaultModelRepository.kt`). It sends:
   - the URL of the model file,
   - a fixed `User-Agent` string identifying the app,
   - an `Accept: application/octet-stream` header,
   - no cookies of our own, and no identifier, account token, advertising ID or device ID.
   The HTTP stack may add the standard headers any HTTPS client sends. Your IP address is visible to the host,
   as it is for any download. The file is streamed to a temporary `.part` file, checked against the SHA-256
   checksum recorded in the app's catalogue, and only installed if the checksum matches.

**Current hosts:** the download URLs in the shipped catalogue point to Google Cloud Storage
(`storage.googleapis.com/mediapipe-models/…`) for the MediaPipe models and to Hugging Face
(`huggingface.co/litert-community/…`) for the MODNet portrait-matting model. Each host processes standard
connection metadata under its own privacy policy. We receive nothing back from either host: no download receipt,
no file list, no identifier.

**There are no other endpoints.** No cloud sync, no remote configuration, no push messaging, no licence check,
no update check, no A/B service, no error-reporting endpoint, and no background traffic of any kind.

## 4. Permissions

The 0.2.3 manifest declares exactly one user-facing permission.

| Permission | Why it is declared | When it is used |
|---|---|---|
| `android.permission.INTERNET` | To download an optional AI model file that you explicitly request | Only during that download; never for media, telemetry, ads or updates |

How the app works without storage, camera or microphone permissions:

- **Media is chosen with the Android system Photo Picker** (`PickVisualMedia` / `PickMultipleVisualMedia`).
  The picker returns access to **only the items you select**; the app cannot browse or index your library.
- **Exports go to your gallery through MediaStore**, which needs no storage permission on Android 10+.
  On Android 7.0–9.0 (API 26–28) the same MediaStore path is used where available, and sharing a file from
  app-specific storage goes through the app's own non-exported FileProvider.
- **No camera and no microphone** are requested: the app imports media through the picker and never records.
- **No location, contacts, calendar, SMS or phone-state** access is requested.

*Technical footnote for reviewers:* the merged manifest also contains
`com.midad.studio.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, a signature-level permission that AndroidX
declares for the app itself so that its dynamically registered broadcast receivers are not exported to other
apps. It is not a runtime permission, it is not grantable to other apps, and it gives access to no user data.
It is not a data-collection permission and it is not requested from the user.

Earlier drafts of this policy described media or storage permissions and an implied backup story. Those
statements did not match the shipped manifest; this version replaces them. The source manifest in
`app/src/main/AndroidManifest.xml` is the authority: it declares `INTERNET` and explicitly removes the media,
storage, foreground-service and wake-lock permissions that libraries would otherwise merge in.

## 5. Local storage, retention and deletion

**Retention.** Everything the app stores is on your device and stays until you remove it:

- Project records, thumbnails and settings: until you delete them in the app or uninstall the app.
- AI model files: until you delete the model in the AI Lab's model manager, or uninstall the app. Deleting a
  model is immediate and cannot be undone; the model can be installed again later.
- Exports: they are ordinary files in your gallery and remain until you delete them with the gallery, a file
  manager or the app's Projects screen (which deletes the export it created).
- **Server-side: none.** There is no retention period because there is no server-side store. We hold no copy
  of your media, your projects, your settings or your exports.

**Deletion.**

- **In-app:** delete a project or an export from Projects; delete an installed AI model in the AI Lab's model
  manager.
- **Uninstall:** removing the app deletes its private storage, including project records, settings, thumbnails
  and installed models. Files you exported to the gallery are not removed by uninstalling — they are yours and
  must be deleted from the gallery.
- **Because nothing is sent to us, there is nothing for us to delete on a server.** If you open a request in
  [GitHub Issues](https://github.com/ahmadmhmdsy/midad-studio/issues) asking us to delete data, we will explain that we hold none and help you delete the local copy.

## 6. Backup, device transfer and copies

- **Android backup is disabled for the app**: `android:allowBackup="false"` in the manifest, and the app's
  data-extraction rules exclude every domain from both cloud backup and device-to-device transfer.
- **Thumbnails** are written to a directory that is excluded from backup.
- **Downloaded or copied AI models** contain no user data; they are re-installable at any time and are not part
  of any backup because backup is off.
- **Your exports** are ordinary gallery files. Whether your own device backup copies them is decided by your
  Google or device backup settings, not by us.

## 7. Children

Midad Studio is a general-purpose photo, video, collage and design editor. It is **not directed to children
under 13** and is not marketed to them. There are no accounts, no ads, no purchases, no social features, no
messaging and no data collection that could identify a child. A child who uses the app edits on the device
only; nothing is sent to us. If you believe a child's personal data has reached us, open a request in
[GitHub Issues](https://github.com/ahmadmhmdsy/midad-studio/issues) and we will act on it; in practice we hold no such data.

## 8. Why we are allowed to process it (lawful basis)

Where the EU/UK GDPR or a similar law applies:

- **Performance of the service you asked for** — Article 6(1)(b): reading the item you pick, letting you edit
  it and writing the export you requested. This happens entirely on your device.
- **Consent** — Article 6(1)(a): the optional model download starts only when you tap download. You can cancel
  it, delete the model afterwards, or use the app with the network off. Withdrawing consent is simply not
  downloading.
- **Legitimate interests** — Article 6(1)(f): keeping the app secure and working, for example verifying a
  model's checksum before use. Because nothing is transmitted to us, this interest is narrow.
- **Legal obligation** — Article 6(1)(c): only if we were compelled to answer a lawful request. We hold no
  server-side data to produce.

**Special-category data.** We do not ask for it. A photo may contain your face, your family, your home or
health information; that content is processed only on your device, at your direction, and is never transmitted
to us.

**No automated decisions.** We do not profile you and no decision about you is automated.

## 9. Your rights

Subject to applicable law, you have the right to:

- **Access** what is held about you — in practice everything in §1, and it is all on your device.
- **Erasure** — delete a project, delete a model, or uninstall the app.
- **Portability** — your exports are standard JPEG, PNG or MP4 files that you can copy anywhere.
- **Restriction, objection or withdrawal of consent** — stop using the optional download, cancel a download,
  delete the model, or use the app with the network off.
- **Complain** to your national data-protection authority.

To exercise a right, open a request in [GitHub Issues](https://github.com/ahmadmhmdsy/midad-studio/issues). We answer within 30 days. Because we operate no server and
hold no identifier, we may be unable to locate any data about you; in that case we will say so plainly rather
than pretend otherwise.

## 10. Security

- **No server, so no server breach.** The strongest protection is that the data never leaves your phone.
- Media access is limited to the items you pick with the system Photo Picker; there is no broad library access.
- App data sits in Android's app-private storage, protected by the operating system's app sandbox.
- Model downloads use **HTTPS only** (redirects to plain HTTP are refused), and each file is verified against a
  **SHA-256** checksum before it is used; a mismatch discards the file.
- Backup and device-transfer extraction are disabled, so the system does not copy app data off the device.
- Release builds are minified with R8 and resource shrinking, only the launcher activity is exported, and the
  FileProvider used for legacy sharing is not exported.
- Limits we cannot remove: a rooted or compromised device, another app with unusually broad access, or files
  you deliberately share through another app.

## 11. International transfers

We do not transfer personal data internationally, because we do not receive any. If you start a model
download, your request goes to the host named in §3, which may be outside your country; that request is made
by you to the host and is governed by the host's own terms and safeguards.

## 12. Third-party components and models

The app includes open-source libraries — LiteRT, Media3, Kotlin, Jetpack Compose and AndroidX — and openly
licensed segmentation models, all under licences that require no data collection. Their licences and
attributions are listed in **Settings → Licenses** and in [MODEL_LICENSES.md](MODEL_LICENSES.md). They are
software components, not data-collecting services: none of them contains analytics, advertising or tracking
code.

A segmentation model that is not bundled with the app is installed only if you install it, and its licence is
supplied by whoever publishes that model — not by us.

## 13. Store and operating-system data

Google Play and Android may collect install, crash, performance and diagnostic information under their own
policies if you install the app through a store. The app ships no analytics or crash SDK, so none of that
reporting comes from instrumentation inside the app, and we do not receive analytics or crash data from it.

## 14. Changes to this policy

- The policy is versioned and dated. This version is 4.0.
- If a future version adds a feature that changes any statement here — an account, cloud sync, a crash SDK,
  analytics, ads, a new permission or a new network call — this policy will be updated **before** that version
  ships, and the in-app privacy text and the published policy will change with it.
- The hosted page is the version of record; the in-app privacy dialog is a summary and links to it.

## 15. Contact and complaints

- **Privacy contact:** the project's [GitHub Issues](https://github.com/ahmadmhmdsy/midad-studio/issues)
- **Publisher:** Ahmad Mahmoud
- **Response time:** within 30 days.
- **Complaints:** you may complain to your national data-protection or consumer authority. If you contact us
  first, we will try to resolve it.

---

## القسم العربي

**سياسة الخصوصية — استوديو مِداد (Midad Studio)**

**إصدار السياسة:** 4.0 · **تاريخ النفاذ:** 2026-11-23 · **تنطبق على:** تطبيق أندرويد «استوديو مِداد»
بحزمة `com.midad.studio`، الإصدار **0.2.3** (رقم البناء **5**).

**أين تُنشر هذه السياسة:** النص نفسه منشور كصفحة مستقلة على
[docs/privacy/index.html](../privacy/index.html) وعلى
https://ahmadmhmdsy.github.io/midad-studio/privacy/، والنسخة المستضافة هي المرجع. وملخّص الخصوصية داخل
التطبيق (الإعدادات ← الخصوصية) إشعار قصير وليس السياسة. ويمكن إرسال أسئلة الخصوصية وطلبات الحذف عبر
[GitHub Issues](https://github.com/ahmadmhmdsy/midad-studio/issues).

### من نحن

- **الناشر / المتحكم بالبيانات:** Ahmad Mahmoud
- **للتواصل بشأن الخصوصية:** [GitHub Issues](https://github.com/ahmadmhmdsy/midad-studio/issues)
- **التطبيق:** استوديو مِداد لأندرويد، الحزمة `com.midad.studio`

لا نُشغّل أي خادم، ولا نملك حسابات مستخدمين، ولا توجد لدينا أي وسيلة للتعرّف عليك من داخل التطبيق. ولا نحتفظ
بأي قاعدة بيانات عن المستخدمين، لأن أي بيانات مستخدم لا تُرسل إلينا أصلاً.

### 1. ما يعالجه التطبيق وأين يبقى

| ما يعالجه | أين يبقى | لماذا | هل يغادر جهازك؟ |
|---|---|---|---|
| الصورة أو الفيديو الذي تختاره | يُقرأ في الذاكرة عبر منتقي الصور في أندرويد أثناء جلسة التحرير | لتتمكن من تحرير ما اخترته | **لا** |
| تعديلك الجاري (الطبقات، الضبط، القص، النص، الرسم) | ذاكرة التطبيق أثناء فتح الاستوديو | ليعمل المحرر والتراجع والإعادة | **لا** |
| الملف المصدَّر (صورة أو فيديو) | معرض الصور في جهازك عبر MediaStore؛ وعلى أندرويد 7.0–9.0 يُسلَّم الملف من تخزين التطبيق الخاص إلى التطبيق الذي تشاركه معه عبر FileProvider غير مُصدَّر | لتسليمك الملف النهائي | **لا**، إلا إذا اخترت مشاركته |
| سجلات المشاريع: الاسم والحجم والتاريخ ومصغّرة صغيرة لآخر التصديرات | التخزين الخاص للتطبيق؛ والمصغّرات في مجلد مستثنى من النسخ الاحتياطي | لتُدرج شاشة «المشاريع» آخر أعمالك | **لا** |
| الإعدادات: السمة واللغة وآخر استوديو واختيارات النماذج | التخزين الخاص للتطبيق | لتذكّر تفضيلاتك | **لا** |
| ملفات نماذج الذكاء الاصطناعي وبصماتها (SHA-256) | مجلد الملفات الخاص بالتطبيق | لتشغيل قصّ الموضوع دون اتصال | **لا** |
| طلب تنزيل النموذج نفسه: عنوان الملف ووقت الطلب وعنوان IP ووكيل المستخدم وحجم البيانات المنقولة | سجلات مضيف التنزيل | لتسليم الملف الذي طلبته | **نعم — إلى ذلك المضيف فقط**، كما في أي تنزيل |

لا نتلقى ولا نرى ولا نخزّن أي بند من البنود الستة الأولى. فالتطبيق لا يملك خادماً يرسلها إليه.

### 2. ما لا نفعله أبداً

- لا حساب ولا تسجيل دخول ولا ملف مستخدم ولا معرّف جهاز.
- لا إعلانات ولا حزمة إعلانية ولا معرّف إعلاني.
- لا تحليلات ولا تتبّع ولا تسجيل أحداث ولا تسجيل جلسات.
- لا حزمة تقارير أعطال: قد يكتب نظام التشغيل سجلاً محلياً عند الانهيار، لكن لا يُرسَل إلينا شيء ولا يمكننا قراءته.
- لا رفع لوسائطك أو مشاريعك أو تصديراتك إلى أي جهة.
- لا بيع ولا تأجير ولا «مشاركة» للبيانات الشخصية.
- لا تنميط ولا قرارات آلية ولا تدريب نماذج على محتواك.
- لا وصول إلى الكاميرا أو الميكروفون أو الموقع أو جهات الاتصال أو التقويم أو الرسائل.

### 3. الشبكة والمستلمون

يُجري التطبيق **نوعاً واحداً من طلبات الشبكة**، ويبدأه أنت فقط:

- **تنزيل اختياري لنموذج ذكاء اصطناعي.** تُقرأ قائمة النماذج من كتالوج مضمَّن في التطبيق
  (`assets/models/manifest.json`). النماذج المضمَّنة موجودة داخل التطبيق ولا تحتاج الشبكة. والنموذج المُعلَّم
  للتنزيل لا يُجلب إلا بعد ضغطك على زر التنزيل: طلب `GET` واحد عبر HTTPS لملف واحد، يرسل عنوان الملف ووكيل
  مستخدم ثابتاً وترويسة `Accept`، دون أي كوكيز خاصة بنا ودون أي معرّف أو رمز حساب. ويظهر عنوان IP الخاص بك
  للمضيف كما في أي تنزيل. يُحفظ الملف مؤقتاً بامتداد `.part`، ويُتحقق من بصمة SHA-256 قبل تثبيته.
- **المضيفون الحاليون:** عناوين التنزيل في الكتالوج المُضمَّن تشير إلى Google Cloud Storage
  (`storage.googleapis.com/mediapipe-models/…`) لنماذج MediaPipe، وإلى Hugging Face
  (`huggingface.co/litert-community/…`) لنموذج MODNet لعزل صورة الشخص. ويعالج كل مضيف بيانات الاتصال
  المعتادة وفق سياسة الخصوصية الخاصة به. ولا نستلم من أي منهما شيئاً.
- **لا نقاط اتصال أخرى:** لا مزامنة سحابية، ولا إعدادات عن بُعد، ولا إشعارات، ولا تحقق من ترخيص، ولا فحص
  تحديثات، ولا أي حركة في الخلفية.

### 4. الأذونات

إصدار 0.2.3 يعلن **إذناً واحداً فقط** موجَّهاً للمستخدم:

| الإذن | لماذا | متى يُستخدم |
|---|---|---|
| `android.permission.INTERNET` | لتنزيل ملف نموذج ذكاء اصطناعي اختياري تطلبه أنت | أثناء ذلك التنزيل فقط؛ ولا يُستخدم للوسائط أو التحليلات أو الإعلانات أو التحديثات |

كيف يعمل التطبيق دون أذونات تخزين أو كاميرا أو ميكروفون:

- **تُختار الوسائط عبر منتقي الصور في أندرويد**، ولا يصل التطبيق إلا إلى العناصر التي تختارها أنت.
- **يُكتب التصدير في معرض الصور عبر MediaStore**، ولا يحتاج ذلك إلى إذن تخزين على أندرويد 10 أو أحدث. وعلى
  أندرويد 7.0–9.0 يُستخدم المسار نفسه حيث يتاح، وتُشارك الملفات عبر FileProvider غير مُصدَّر خاص بالتطبيق.
- **لا كاميرا ولا ميكروفون:** الاستيراد عبر المنتقي، والتطبيق لا يسجّل.
- **لا موقع ولا جهات اتصال ولا تقويم ولا رسائل ولا حالة هاتف.**

*ملاحظة تقنية للمراجعين:* يتضمن البيان المدمج أيضاً
`com.midad.studio.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`، وهو إذن على مستوى التوقيع تعلنه AndroidX
للتطبيق نفسه كي لا تُصدَّر مستقبلات البث المسجّلة ديناميكياً إلى التطبيقات الأخرى. ليس إذناً وقت التشغيل، ولا
يُمنح لتطبيقات أخرى، ولا يتيح أي بيانات مستخدم.

### 5. التخزين المحلي والاحتفاظ والحذف

- تُحفظ سجلات المشاريع والمصغّرات والإعدادات حتى تحذفها من التطبيق أو تُزيل التطبيق.
- تُحفظ نماذج الذكاء الاصطناعي حتى تحذف النموذج من مدير النماذج في «مختبر الذكاء» أو تُزيل التطبيق.
- تبقى التصديرات في معرض الصور حتى تحذفها من المعرض أو من مدير الملفات أو من شاشة «المشاريع».
- **لا يوجد احتفاظ على أي خادم:** لا نخزّن نسخة من وسائطك أو مشاريعك أو إعداداتك.
- **الحذف من داخل التطبيق:** احذف المشروع أو التصدير من «المشاريع»، واحذف النموذج من مدير النماذج.
- **إزالة التطبيق:** تحذف تخزينه الخاص كاملاً، بينما تبقى الملفات التي صدّرتها إلى المعرض ملكاً لك.

### 6. النسخ الاحتياطي والنقل بين الأجهزة

- **النسخ الاحتياطي في أندرويد مُعطَّل** (`android:allowBackup="false"`)، وقواعد استخراج البيانات تستثني جميع
  النطاقات من النسخ السحابي ومن النقل بين الأجهزة.
- **المصغّرات** تُكتب في مجلد مستثنى من النسخ الاحتياطي.
- **النماذج** لا تحتوي بيانات مستخدم، ويمكن إعادة تثبيتها، ولا تُنسخ لأن النسخ معطّل.
- **تصديراتك** ملفات عادية في المعرض، ويقرر إعداد النسخ الاحتياطي في جهازك ما إذا كانت تُنسخ، لا نحن.

### 7. الأطفال

«استوديو مِداد» محرر عام للصور والفيديو والكولاج والتصميم، و**ليس موجَّهاً إلى الأطفال دون 13 عاماً** ولا
يُسوَّق لهم. لا حسابات ولا إعلانات ولا مشتريات ولا ميزات اجتماعية ولا رسائل ولا جمع بيانات تعرّف بالطفل. ومن
يستخدمه يحرّر على الجهاز فقط، ولا يُرسل إلينا شيء. وإذا اعتقدت أن بيانات طفل وصلت إلينا فافتح طلباً في
[GitHub Issues](https://github.com/ahmadmhmdsy/midad-studio/issues).

### 8. الأساس القانوني للمعالجة

حيث ينطبق نظام حماية البيانات الأوروبي (GDPR) أو ما يشبهه:

- **تنفيذ الخدمة التي طلبتها** — المادة 6(1)(b): قراءة ما تختاره وتحريره وكتابة التصدير، وذلك على جهازك بالكامل.
- **موافقتك** — المادة 6(1)(a): لا يبدأ تنزيل النموذج إلا بضغطك، ويمكنك إلغاؤه أو حذف النموذج لاحقاً.
- **المصلحة المشروعة** — المادة 6(1)(f): إبقاء التطبيق آمناً وعاملاً، مثل التحقق من بصمة النموذج. وهي مصلحة
  ضيقة لأن شيئاً لا يُرسل إلينا.
- **الالتزام القانوني** — المادة 6(1)(c): فقط إذا أُلزمنا قانوناً بالرد، ولا توجد لدينا بيانات خادم نقدّمها.

**البيانات الحساسة:** لا نطلبها. قد تحتوي صورتك على وجهك أو عائلتك أو منزلك أو معلومات صحية، لكن هذا المحتوى
يُعالج على جهازك فقط وبناءً على توجيهك، ولا يُرسل إلينا أبداً.

### 9. حقوقك

بحسب القانون المعمول به، لك الحق في:

- **الوصول** إلى ما يُحتفظ عنك، وهو كل ما في البند 1، وموجود على جهازك.
- **المحو** بحذف مشروع أو نموذج أو بإزالة التطبيق.
- **النقل** لأن تصديراتك ملفات JPEG أو PNG أو MP4 قياسية.
- **تقييد المعالجة أو الاعتراض أو سحب الموافقة** بالتوقف عن التنزيل الاختياري أو حذف النموذج أو استخدام التطبيق
  دون اتصال.
- **التقدّم بشكوى** إلى هيئة حماية البيانات في بلدك.

لممارسة حقوقك افتح طلباً في [GitHub Issues](https://github.com/ahmadmhmdsy/midad-studio/issues)، وسنرد خلال
30 يوماً. ولأننا لا نُشغّل خادماً ولا نحتفظ بمعرّف، قد لا نتمكن من تحديد أي بيانات عنك، وسنصارحك بذلك بدل
الادعاء بغير الحقيقة.

### 10. الأمان

- **لا خادم، فلا اختراق لخادم.** الحماية الأقوى هي أن البيانات لا تغادر هاتفك.
- الوصول إلى الوسائط مقصور على ما تختاره في منتقي الصور.
- بيانات التطبيق في تخزينه الخاص المحمي بصندوق أندرويد.
- التنزيل عبر **HTTPS فقط** (وتُرفض إعادة التوجيه إلى HTTP)، مع التحقق من **SHA-256** قبل الاستخدام.
- النسخ الاحتياطي والنقل بين الأجهزة معطّلان.
- إصدارات الإصدار مضغوطة بـ R8 مع تقليص الموارد، ولا يُصدَّر سوى نشاط التشغيل، وFileProvider غير مُصدَّر.
- حدود لا يمكننا إزالتها: جهاز مُروَّت أو مخترق، تطبيق آخر بصلاحيات واسعة، أو ملف تشاركه أنت مع تطبيق آخر.

### 11. النقل الدولي

لا ننقل بيانات شخصية دولياً لأننا لا نتلقى أي بيانات. وإذا بدأت تنزيل نموذج، فإن طلبك يذهب إلى المضيف
المذكور في البند 3، وقد يكون خارج بلدك، وذلك الطلب تصنعه أنت للمضيف وتنظمه شروطه.

### 12. المكوّنات والنماذج الخارجية

يضمّ التطبيق مكتبات مفتوحة المصدر — LiteRT وMedia3 وKotlin وJetpack Compose وAndroidX — ونماذج تقطيع
مفتوحة الترخيص لا تجمع بيانات. تراخيصها وإسناداتها في **الإعدادات ← التراخيص** وفي
[MODEL_LICENSES.md](MODEL_LICENSES.md). وهي مكوّنات برمجية لا خدمات تجمع بيانات.

أي نموذج تقطيع غير مضمَّن في التطبيق لا يُثبَّت إلا إذا ثبّتته أنت، وترخيصه يوفّره ناشره لا نحن.

### 13. بيانات المتجر والنظام

قد تجمع Google Play وأندرويد معلومات التثبيت والأعطال والأداء وفق سياساتها إذا ثبّتت التطبيق من متجر. ولا
يضمّ التطبيق أي حزمة تحليلات أو تقارير أعطال، فليس في تلك التقارير شيء من قياسات داخل التطبيق، ولا نتلقى
نحن تحليلات أو تقارير أعطال من التطبيق.

### 14. التغييرات

- السياسة مُصدَّرة ومؤرَّخة، وهذه النسخة 4.0.
- إذا أضاف إصدار مستقبلي ميزة تغيّر أي عبارة هنا — حساب أو مزامنة أو حزمة أعطال أو تحليلات أو إعلانات أو إذناً
  جديداً أو اتصالاً جديداً — فسيُحدَّث هذا المستند **قبل** إصدار ذلك الإصدار، مع نص الخصوصية داخل التطبيق
  وهذه السياسة المنشورة.
- الصفحة المستضافة هي النسخة المرجعية، وملخّص الخصوصية داخل التطبيق يوجزها ويحيل إليها.

### 15. التواصل والشكاوى

- **للتواصل:** [GitHub Issues](https://github.com/ahmadmhmdsy/midad-studio/issues)
- **الناشر:** Ahmad Mahmoud
- **مدة الرد:** خلال 30 يوماً.
- **الشكاوى:** يمكنك اللجوء إلى هيئة حماية البيانات أو المستهلك في بلدك، ويسعدنا أن نحاول الحل أولاً.

---

## Annex A — Google Play Data Safety summary

| Google Play Data safety question | Answer for 0.2.3 | Why |
|---|---|---|
| Does the app collect or share any required user data type? | **No** | Nothing is transmitted off the device to us or to a third party; the only request is a model-file fetch that carries no user data |
| Data shared with third parties | **None** | No SDK, no server, no sale |
| Encrypted in transit | Not applicable — no data collected; the optional model download uses HTTPS | §3 |
| Users can request data deletion | Not applicable — no server-side data; in-app delete and uninstall remove everything local | §5, §9 |
| Ads, analytics, crash reporting | **No** | No ad, analytics or crash SDK; no advertising ID |

The full field-by-field form answers are in [DATA_SAFETY.md](DATA_SAFETY.md).

## Annex B — Version history

| Version | Date | Change |
|---|---|---|
| 4.0 | 2026-11-23 | First published version, for 0.2.3 (versionCode 5): added the second download host (Hugging Face for MODNet) and the six-model catalogue; version references and contact details updated. |
| 3.0 | 2026-11-17 | Rewritten for 0.2.0 (versionCode 2): the single model-download request documented with its host and headers; permissions corrected to `INTERNET` only plus the AndroidX signature permission footnote; retention and deletion made explicit; full Arabic section added. |
| 2.0 | 2026-11-17 | First 0.2.0 draft: permissions corrected, backup stated as disabled, storage-permission claims removed. Superseded before publication. |
| 1.0-draft | 2026-11-16 | First draft: described media/storage permissions and an implied backup story that did not match the shipped manifest; superseded. |

---


