# 🎤 VPS Vosk Processing Speech

**Превращаем бормотание в текст быстрее, чем вы успеваете сказать "Ой, это не то!"**

<img src="https://alphacephei.com/img/logo.png" alt="Vosk Logo" style="filter: contrast(2) brightness(3);">

*Powered by [Vosk](https://alphacephei.com/vosk/) – offline speech recognition for hackers & rebels.*


---

## 🚀 **Что это?**
Это сервис для обработки речи на VPS с использованием **Vosk** – оффлайн-движка распознавания речи.
- 🔥 **Полностью оффлайн** (модели работают локально, но сам сервер, конечно, требует подключения)
- 🏎 **Быстро и точно** (если вы не говорите как пьяный Джарвис).
- 📂 **Поддержка множества языков** (даже эльфийский, если обучить модель).

---

## ⚙️ **Установка**

```bash
# Клонируем репозиторий (если вы ещё этого не сделали)
git clone https://github.com/ThomasWilson1903/VoskAsync.git
cd VoskAsync

# Собираем проект
mvn clean install

# Качаем модель Vosk (например, русскую маленькую)
wget https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip
unzip vosk-model-small-ru-0.22.zip && mv vosk-model-small-ru-0.22 model
