# 🎤 VPS Vosk Processing Speech

**We turn mumbling into text faster than you can say "Oh, that's not it!"**

<img src="https://alphacephei.com/img/logo.png" alt="Vosk Logo" style="filter: contrast(2) brightness(3);">

*Powered by [Vosk](https://alphacephei.com/vosk/) – offline speech recognition for hackers & rebels.*


---

## 🚀 **Что это?**
Это сервис для обработки речи на VPS с использованием **Vosk**

- 🔥 **Модели работают локально** 
- 🏎 **Быстро и точно**
- 📂 **Поддержка множества языков** (даже эльфийский, если обучить модель).

---

## ⚙️ **Установка**

```bash
# Клонируем репозиторий
git clone https://github.com/ThomasWilson1903/VoskAsync.git
cd VoskAsync

# Собираем проект
mvn clean install

# Качаем модель Vosk (например, русскую маленькую)
cd vosk-models
wget https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip
unzip vosk-model-small-ru-0.22.zip && mv vosk-model-small-ru-0.22 model

# Запуск
docker-compose up -d --build
```
