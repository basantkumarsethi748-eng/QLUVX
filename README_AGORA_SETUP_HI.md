# QLUVX Agora Voice Room — लागू करने के निर्देश

यह पैच मौजूदा `com.example.qluvx` Java/XML ऐप को बदलने के लिए है; यह अलग demo app नहीं है।

## शामिल बदलाव
- Agora RTC Voice SDK
- Agora Maven repository (`https://download.agora.io/maven/`)
- Microphone/network permissions और `LiveAudioRoomActivity` + `VoiceRoomActivity` (12-Seat XML GridLayout)
- QLUVX Home से Voice Room बटन का वास्तविक navigation
- Join / mute / speaker / leave controls और runtime microphone permission
- ProGuard rules (`app/proguard-rules.pro`)

## Android फोन से लागू करना
1. GitHub पर `basantsethi46-create/QLUVX` खोलें: https://github.com/basantsethi46-create/QLUVX
2. ZIP से फाइलें उसी path पर upload/replace करें; `README_AGORA_SETUP_HI.md` केवल निर्देश है।
3. GitHub Actions/cloud build चलाकर build log देखें।

## जरूरी Agora सेटअप
1. Agora Console में project बनाकर App ID लें।
2. AI Studio Secrets पैनल में `AGORA_APP_ID` सेट करें और ऐप में Voice Room खोलकर room/channel name डालें।
3. खाली token केवल Agora के testing-mode project में काम कर सकता है। Production में token backend server से generate करें; App Certificate कभी Android app में न रखें।

Official docs: https://docs.agora.io/en/realtime-media/rtc/get-started-sdk?platform=android
