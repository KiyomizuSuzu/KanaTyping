<details><summary>日本語</summary>

# KanaTyping
Kotlinで開発された、日本語のかな入力練習用Androidアプリです。Android標準のUIコンポーネントを使用しています。

### ビルド方法
以下のツールがインストールされていることを確認してください。
- [Android CLI](https://developer.android.com/tools/agents/android-cli/download#windows)
- [Java SE Development Kit 27](https://www.oracle.com/asean/java/technologies/downloads/#jdk27-windows)
- Android SDK：PowerShellで `android sdk install platforms/android-36 build-tools platform-tools` を実行してインストールしてください。

リポジトリのルートディレクトリでPowerShellを開き、以下のコマンドを実行すると、署名済みのリリースAPKをビルドできます。
```powershell
.\gradlew.bat assembleRelease
```
## AGPL-3.0 ライセンス
参照：https://licenses.opensource.jp/AGPL-3.0/AGPL-3.0.html

[OSI承認済み](https://opensource.org/licenses?ls=GNU+Affero+General+Public+License+version+3)のオープンソースライセンス。AGPL-3.0の条件のもとで、自由にフォーク・改変・再配布してもらって構わない。

AGPL-3.0に従う以上、対象コードは同じライセンスのまま維持する必要があり、別のライセンスへの再ライセンスはできない。また、このソフトウェアを受け取った人（購入やサービス経由も含む）には、同じライセンス条件のもとで対応するソースコードへのアクセスを提供する必要がある。

ライセンス全文は[LICENSE.txt](https://github.com/KiyomizuSuzu/KanaTyping/blob/main/LICENSE.txt)を参照。
</details>

---
<details open><summary>English</summary>

# KanaTyping
An Android application built with Kotlin for practising Japanese kana typing and uses Android's native UI components.

### To Build the Source Code
Ensure you have the following installed:
- [Android CLI](https://developer.android.com/tools/agents/android-cli/download#windows)
- [Java SE Development Kit 27](https://www.oracle.com/asean/java/technologies/downloads/#jdk27-windows)
- Android SDK by running `android sdk install platforms/android-36 build-tools platform-tools` in Powershell.

Open PowerShell in the repository root directory and run the following command to build an automatically signed release APK:
```powershell
.\gradlew.bat assembleRelease
```

## AGPL-3.0 license
Source: https://www.gnu.org/licenses/agpl-3.0.en.html

This is an [OSI-approved](https://opensource.org/licenses?ls=GNU+Affero+General+Public+License+version+3) open-source license. Free to fork, modify, and redistribute under the terms of the AGPL-3.0.

By complying with the AGPL-3.0 license, you must keep the same license for the covered work and cannot relicense that covered part under a different license.
Anyone who receives the software (including through purchase or as a service) must also be provided access to the corresponding source code under the same license.

See the [LICENSE.txt](https://github.com/KiyomizuSuzu/KanaTyping/blob/main/LICENSE.txt) for the full license text.
</details>
