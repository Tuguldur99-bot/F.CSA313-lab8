# Lab08 - Mock объектууд: Mockito-гоор хамаарлыг орлуулж тестлэх

Нэр: Ц.Төгөлдөр Оюутны код: B232270050

## Орчин

```
$ java -version
openjdk version "17.0.20.1" 2026-08-18
OpenJDK Runtime Environment (build 17.0.20.1+1-1-24.04-Ubuntu)
OpenJDK 64-Bit Server VM (build 17.0.20.1+1-1-24.04-Ubuntu, mixed mode, sharing)
```

```
$ mvn -version
Apache Maven 3.9.11 (3e54c93a704957b63ee3494413a2b544fd3d825b)
Maven home: /mnt/d/Program Files/Apache/Maven/apache-maven-3.9.11
Java version: 17.0.20.1, vendor: Ubuntu, runtime: /usr/lib/jvm/java-17-openjdk-amd64
Default locale: en, platform encoding: UTF-8
OS name: "linux", version: "6.18.54.1-microsoft-standard-wsl2", arch: "amd64", family: "unix"
```

## Тойм

`CourseRegistrationService` классыг хоёр хамаарлаас (`StudentRepository`, `EmailSender`) тусгаарлаж, Mockito-гоор орлуулан нэгжийн түвшинд тестэлсэн. Хоёр интерфейст бодит хэрэгжүүлэлт байхгүй тул өгөгдлийн сан, SMTP сервергүйгээр бизнес логикоо шалгасан. Тестийн кодыг AI туслахтай хамтран бичсэн; би өөрөө ажиллуулж, Хэсэг B-ийн гурван мутантаар эвдэж шалгасан.

## Хэсэг A: тестүүд

Тестийн тоо: **10** - `results/mvn-test.txt`-ийн сүүлийн мөр `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. Энэ нь 7 энгийн тест (A1-A7) + A8-ийн 3 мөр (мөр бүр тусдаа тест болж тоологдсон).

| № | Тест (`CourseRegistrationServiceTest`) | Гол хэрэгсэл |
|---|---|---|
| A1 | `register_activeStudentWithPrerequisitesMet_returnsOkSavesAndSendsEmail` | `when` + `verify` |
| A2 | `register_unknownStudent_returnsNoStudentAndDoesNotSaveOrEmail` | `verify(..., never())` |
| A3 | `register_inactiveStudent_returnsInactiveAndNeverTouchesEmailSender` | `verifyNoInteractions` |
| A4 | `register_missingPrerequisite_returnsPrerequisitesErrorAndDoesNotSave` | `when` + `never` |
| A5 | `register_emailSenderThrows_stillReturnsOkAndSaves` | `doThrow(...).when(...)` |
| A6 | `register_nullArgument_throwsIllegalArgumentAndTouchesNothing` | `assertThrows` (null studentId ба null courseId тусдаа) |
| A7 | `register_success_emailBodyContainsCourseCode` | `ArgumentCaptor` |
| A8 | `register_errorClass_returnsExpectedErrorAndDoesNotSaveOrEmail` (3 мөр: noStudent, inactive, noCourse) | `@ParameterizedTest` + `@CsvSource` |

**A8-ийн `lenient()`.** A8-ийн мөр бүр өөр stub-ийн хослол ашигладаг (жишээ нь `noStudent` мөрөнд `courseExists` stub хэзээ ч дуудагдахгүй), харин MockitoExtension-ий strict stubs горим ашиглагдаагүй stub-ийг `UnnecessaryStubbingException`-аар унагадаг. Иймд A8-ийн stub-уудыг `lenient().when(...)` хэлбэрээр бичсэн.

## Хэсэг B: мини мутаци

Мутант бүрийн дараа кодыг `git checkout`-оор эх хувилбараар нь сэргээж, `results/mvn-test-mutant-N.txt`-д гаралтыг хадгалсан.

| Мутант | Эвдрэл | Унасан тестүүд | Үр дүн (`results/`) |
|---|---|---|---|
| 1 | идэвхгүй оюутны шалгалт (`if (!student.active()) ...`) комментлосон | A3 `register_inactiveStudent_...` (ERROR_NO_COURSE буцсан), A8-ийн 2-р мөр `inactive` (OK буцсан) | `mvn-test-mutant-1.txt`: Tests run: 10, Failures: 2, Errors: 0, BUILD FAILURE |
| 2 | `saveRegistration(...)` дуудалт комментлосон | A1 `register_activeStudentWithPrerequisitesMet_...`, A5 `register_emailSenderThrows_...` (saveRegistration "wanted but not invoked") | `mvn-test-mutant-2.txt`: Tests run: 10, Failures: 2, Errors: 0, BUILD FAILURE |
| 3 | `|| courseId == null` хэсгийг устгасан | A6 `register_nullArgument_...` (`register("S1", null)` exception шидээгүй) | `mvn-test-mutant-3.txt`: Tests run: 10, Failures: 1, Errors: 0, BUILD FAILURE |

Гурван мутант бүгд баригдсан, `UnnecessaryStubbingException` гараагүй. Мутант 3 нэг тестийн нэг мөрөнд баригдсан: A6 нь `null` studentId ба `null` courseId-г тусдаа `assertThrows`-оор шалгадаг тул цоорхой олдоогүй, A6-г засах шаардлагагүй байв. Мутаци бүрийн дараа код сэргээгдэж, эцэст нь `results/mvn-test.txt` дахин ногоон (`Tests run: 10, Failures: 0`, `BUILD SUCCESS`) болсон.

## Дүгнэлт

Mock нь хамаарлын дуудалтыг бүртгэж, "энэ метод дуудагдсан уу, ямар аргументтай" гэдгийг verify хийх боломж олгодог бол гараар бичсэн fake нь жижиг боловч ажилладаг хэрэгжүүлэлт (жишээ нь санах ойн Map) тул төлөвөөр (state) шалгахад тохиромжтой, гэхдээ түүнийг өөрөө бичиж засварлах хэрэгтэй болдог. Лабораторид хоёр интерфейст бодит хэрэгжүүлэлт байхгүй байсан тул fake бичихээс илүү mock нь хурдан бөгөөд тестийг сан, SMTP сервертэй холбохгүй байв. Mock хийх ёсгүй зүйл бол тестлэж буй классаа өөрийг нь, мөн `Student` шиг энгийн өгөгдлийн объект (record) юм: тэднийг бодитоор үүсгэх нь хялбар бөгөөд mock хийвэл тест юу ч шалгахгүй болно. Мөн өөрийн мэдэхгүй гадаад сангийн классыг шууд mock хийхээс зайлсхийж, өөрийн интерфейсээр хүрээлэх нь дээр. `verify`-г хэтрүүлбэл тест кодын дотоод хэрэгжүүлэлттэй чанга холбогдож, зөв ажиллах кодыг refactor хийхэд л тест унадаг болно. Үүний жишээ нь манай A8: мөр бүр өөр stub ашигладаг тул `lenient()` хэрэглэх хэрэгтэй болсон нь тест кодын замтай хэр хүчтэй холбогддогийг харуулсан. Хэсэг B-ийн мутаци нь тест бичсэн гэдэг нь тест "ажилладаг" гэсэн үг биш гэдгийг батлах хамгийн энгийн арга болсон: гурван мутант бүгд баригдсан нь тестүүд хийсвэр биш, бодит зан төлвийг шалгаж байгааг харуулсан.

## Репозиторийн бүтэц

```
pom.xml
src/main/java/mn/edu/must/sqat/   # starter (өөрчлөгдөөгүй)
src/test/java/mn/edu/must/sqat/CourseRegistrationServiceTest.java
results/mvn-test.txt              # ногоон, Tests run: 10
results/mvn-test-mutant-1.txt     # Failures: 2
results/mvn-test-mutant-2.txt     # Failures: 2
results/mvn-test-mutant-3.txt     # Failures: 1
README.md
.gitignore
```
