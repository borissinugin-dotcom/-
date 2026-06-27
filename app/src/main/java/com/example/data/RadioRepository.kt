package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RadioRepository(private val radioDao: RadioDao) {

    val allStations: Flow<List<RadioStation>> = radioDao.getAllStations()
    val favoriteStations: Flow<List<RadioStation>> = radioDao.getFavoriteStations()
    val allRecordings: Flow<List<Recording>> = radioDao.getAllRecordings()

    suspend fun insertStation(station: RadioStation) = withContext(Dispatchers.IO) {
        radioDao.insertStation(station)
    }

    suspend fun updateStation(station: RadioStation) = withContext(Dispatchers.IO) {
        radioDao.updateStation(station)
    }

    suspend fun deleteStation(station: RadioStation) = withContext(Dispatchers.IO) {
        radioDao.deleteStation(station)
    }

    suspend fun insertRecording(recording: Recording) = withContext(Dispatchers.IO) {
        radioDao.insertRecording(recording)
    }

    suspend fun deleteRecording(recording: Recording) = withContext(Dispatchers.IO) {
        radioDao.deleteRecording(recording)
    }

    suspend fun prepopulateIfNeeded() = withContext(Dispatchers.IO) {
        val currentStations = radioDao.getAllStations().first()
        if (currentStations.size < 50) {
            // Delete old pre-populated default stations to replace with the comprehensive 100+ Russian list
            for (station in currentStations) {
                if (!station.isCustom) {
                    radioDao.deleteStation(station)
                }
            }

            val defaults = listOf(
                // Pop & Hits
                RadioStation(name = "Европа Плюс", streamUrl = "https://ep128.hostingradio.ru:8030/ep128", genre = "Поп"),
                RadioStation(name = "Русское Радио", streamUrl = "https://rusradio.hostingradio.ru/rusradio128.mp3", genre = "Поп"),
                RadioStation(name = "Авторадио", streamUrl = "http://avtoradio.hostingradio.ru/ar128.mp3", genre = "Поп"),
                RadioStation(name = "Лав Радио (Love Radio)", streamUrl = "http://loveradio.hostingradio.ru:8000/Love_128", genre = "Поп"),
                RadioStation(name = "Хит ФМ (Hit FM)", streamUrl = "https://hitfm.hostingradio.ru/hitfm128.mp3", genre = "Поп"),
                RadioStation(name = "ДФМ (DFM)", streamUrl = "https://dfm.hostingradio.ru/dfm128.mp3", genre = "Клубная / Поп"),
                RadioStation(name = "Радио Энергия (Energy)", streamUrl = "http://energy.hostingradio.ru/energy128.mp3", genre = "Поп"),
                RadioStation(name = "Лайк ФМ (Like FM)", streamUrl = "http://likefm.hostingradio.ru/likefm128.mp3", genre = "Поп"),
                RadioStation(name = "Радио Романтика", streamUrl = "http://romantika.hostingradio.ru/romantika128.mp3", genre = "Поп"),
                RadioStation(name = "Радио 7 на семи холмах", streamUrl = "https://radio7.hostingradio.ru:8013/radio7128", genre = "Поп"),
                RadioStation(name = "Новое Радио", streamUrl = "http://icecast.newradio.cdnvideo.ru/newradio3", genre = "Поп"),
                RadioStation(name = "Радио Дача", streamUrl = "http://radiodacha.hostingradio.ru:8000/rd_128", genre = "Поп"),
                RadioStation(name = "Жара ФМ", streamUrl = "http://zharafm.hostingradio.ru/zharafm128.mp3", genre = "Поп"),
                RadioStation(name = "ТНТ Музыка", streamUrl = "https://tntmusic.hostingradio.ru/tntmusic128.mp3", genre = "Поп"),
                RadioStation(name = "Студия 21 (Studio 21)", streamUrl = "http://icecast.studio21.cdnvideo.ru/studio21_128", genre = "Рэп / Хип-Хоп"),
                RadioStation(name = "Страна ФМ", streamUrl = "https://stranafm.hostingradio.ru/stranafm128.mp3", genre = "Русский Поп"),
                RadioStation(name = "Радио Ваня", streamUrl = "https://radiovanya.hostingradio.ru/radiovanya128.mp3", genre = "Русский Поп"),
                RadioStation(name = "Радио Метро (Metro FM)", streamUrl = "http://stream.radiometro.ru:8000/radio-metro-128", genre = "Поп"),
                RadioStation(name = "Русский Хит", streamUrl = "http://ruhit.hostingradio.ru:8000/ruhit_128", genre = "Русский Поп"),
                RadioStation(name = "Суббота ФМ", streamUrl = "http://subbotafm.hostingradio.ru/subbotafm128.mp3", genre = "Поп"),

                // Retro & Chanson
                RadioStation(name = "Ретро ФМ", streamUrl = "https://retro128.hostingradio.ru:8014/retro128", genre = "Ретро"),
                RadioStation(name = "Дорожное Радио", streamUrl = "https://dorognoe.hostingradio.ru:8000/dorognoe_128", genre = "Ретро"),
                RadioStation(name = "Радио Шансон", streamUrl = "http://chanson.hostingradio.ru:8001/chanson128", genre = "Шансон"),
                RadioStation(name = "Серебряный Дождь", streamUrl = "http://95.163.74.205:8000/stream", genre = "Разговорное"),
                RadioStation(name = "Радио Маяк", streamUrl = "http://icecast.vgtrk.cdnvideo.ru/mayakfm_mp3_128kbps", genre = "Разговорное / Ретро"),
                RadioStation(name = "Комсомольская Правда", streamUrl = "http://95.163.74.246:8000/kp128.mp3", genre = "Разговорное"),
                RadioStation(name = "Радио России", streamUrl = "http://icecast.vgtrk.cdnvideo.ru/radiorossii_mp3_128kbps", genre = "Разговорное"),
                RadioStation(name = "Радио Вера", streamUrl = "http://stream.radiovera.ru/vera-128.mp3", genre = "Разговорное"),
                RadioStation(name = "Радио Мелодия (СССР)", streamUrl = "http://195.182.132.18:8000/melodia128", genre = "Ретро"),
                RadioStation(name = "Ностальгия ФМ", streamUrl = "http://stream.nostalgia.su:8000/nostalgia128", genre = "Ретро"),
                RadioStation(name = "Град Петров", streamUrl = "http://93.189.144.130:8000/gradpetrov128", genre = "Разговорное"),
                RadioStation(name = "Восток ФМ", streamUrl = "http://vostokfm.hostingradio.ru:8000/vostokfm_128", genre = "Поп"),
                RadioStation(name = "Радио Родина", streamUrl = "http://rodinafm.hostingradio.ru/rodinafm128.mp3", genre = "Ретро"),
                RadioStation(name = "Шансон Без Цензуры", streamUrl = "https://chanson.hostingradio.ru/chanson_free128.mp3", genre = "Шансон"),
                RadioStation(name = "Казак ФМ", streamUrl = "http://83.166.241.135:8000/stream", genre = "Фолк"),

                // Rock & Alternative
                RadioStation(name = "Наше Радио", streamUrl = "https://nashe.hostingradio.ru/nashe-128.mp3", genre = "Рок"),
                RadioStation(name = "Радио Максимум", streamUrl = "https://maximum.hostingradio.ru/maximum128.mp3", genre = "Рок"),
                RadioStation(name = "Рок ФМ (Rock FM)", streamUrl = "https://rockfm.hostingradio.ru/rockfm128", genre = "Рок"),
                RadioStation(name = "Радио Ультра (Ultra)", streamUrl = "https://ultra.hostingradio.ru/ultra-128.mp3", genre = "Альтернатива / Рок"),
                RadioStation(name = "Record Rock", streamUrl = "https://radiorecord.hostingradio.ru/rock_128.mp3", genre = "Рок"),
                RadioStation(name = "Джаз-Рок", streamUrl = "http://jazzlab.hostingradio.ru:8015/jazz_rock-128.mp3", genre = "Рок"),
                RadioStation(name = "Кекс ФМ Рок", streamUrl = "http://keksfm.hostingradio.ru/keksfm128.mp3", genre = "Рок"),
                RadioStation(name = "Высокое Напряжение (Metal)", streamUrl = "https://radiorecord.hostingradio.ru/highvoltage_128.mp3", genre = "Рок"),
                RadioStation(name = "Хард-н-Хеви", streamUrl = "https://radiorecord.hostingradio.ru/hardnhappy_128.mp3", genre = "Рок"),
                RadioStation(name = "Готик Рок (Goth Rock)", streamUrl = "https://radiorecord.hostingradio.ru/goth_128.mp3", genre = "Рок"),
                RadioStation(name = "Панк-рок (Punk)", streamUrl = "https://radiorecord.hostingradio.ru/punk_128.mp3", genre = "Рок"),
                RadioStation(name = "Гранж (Grunge)", streamUrl = "https://radiorecord.hostingradio.ru/grunge_128.mp3", genre = "Рок"),
                RadioStation(name = "Рок-н-Ролл (Rock & Roll)", streamUrl = "https://radiorecord.hostingradio.ru/rocknroll_128.mp3", genre = "Рок"),
                RadioStation(name = "Инди-рок (Indie Rock)", streamUrl = "https://radiorecord.hostingradio.ru/indie_128.mp3", genre = "Рок"),

                // Lounge & Relax & Jazz
                RadioStation(name = "Радио Монте-Карло", streamUrl = "https://montecarlo.hostingradio.ru/montecarlo128.mp3", genre = "Лаунж"),
                RadioStation(name = "Релакс ФМ (Relax FM)", streamUrl = "http://relaxfm.hostingradio.ru/relaxfm128.mp3", genre = "Релакс"),
                RadioStation(name = "Радио Джаз (Radio Jazz)", streamUrl = "http://jazzlab.hostingradio.ru:8015/jazz-128.mp3", genre = "Джаз"),
                RadioStation(name = "Орфей (Классика)", streamUrl = "http://89.208.99.16:8088/orpheus_128", genre = "Классика"),
                RadioStation(name = "Радио Классика", streamUrl = "http://jazzlab.hostingradio.ru:8015/classic-128.mp3", genre = "Классика"),
                RadioStation(name = "Смуз-Джаз (Smooth Jazz)", streamUrl = "http://jazzlab.hostingradio.ru:8015/smooth-128.mp3", genre = "Джаз"),
                RadioStation(name = "Радио Блюз (Blues)", streamUrl = "http://jazzlab.hostingradio.ru:8015/blues-128.mp3", genre = "Блюз"),
                RadioStation(name = "Вокальный Джаз", streamUrl = "http://jazzlab.hostingradio.ru:8015/vocal-128.mp3", genre = "Джаз"),
                RadioStation(name = "Классический Джаз", streamUrl = "http://jazzlab.hostingradio.ru:8015/classic_jazz-128.mp3", genre = "Джаз"),
                RadioStation(name = "Chillwave", streamUrl = "https://radiorecord.hostingradio.ru/chillwave_128.mp3", genre = "Релакс"),
                RadioStation(name = "Synthwave", streamUrl = "https://radiorecord.hostingradio.ru/synth_128.mp3", genre = "Релакс"),

                // Club / Dance (Radio Record)
                RadioStation(name = "Record Club", streamUrl = "https://radiorecord.hostingradio.ru/club_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Russian Mix", streamUrl = "https://radiorecord.hostingradio.ru/rus_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Dubstep", streamUrl = "https://radiorecord.hostingradio.ru/dub_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Trap", streamUrl = "https://radiorecord.hostingradio.ru/trap_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Pirate Station", streamUrl = "https://radiorecord.hostingradio.ru/ps_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Vip House", streamUrl = "https://radiorecord.hostingradio.ru/vip_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Deep House", streamUrl = "https://radiorecord.hostingradio.ru/deep_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Techno", streamUrl = "https://radiorecord.hostingradio.ru/techno_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Trancemission", streamUrl = "https://radiorecord.hostingradio.ru/tm_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Hardstyle", streamUrl = "https://radiorecord.hostingradio.ru/hard_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Minimal Tech", streamUrl = "https://radiorecord.hostingradio.ru/mini_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Old School Rave", streamUrl = "https://radiorecord.hostingradio.ru/rave_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Electro", streamUrl = "https://radiorecord.hostingradio.ru/elect_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record 90s Dance", streamUrl = "https://radiorecord.hostingradio.ru/sd90_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record 80s Dance", streamUrl = "https://radiorecord.hostingradio.ru/groove_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Dancecore", streamUrl = "https://radiorecord.hostingradio.ru/dc_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Liquid Funk", streamUrl = "https://radiorecord.hostingradio.ru/liquidfunk_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Tecktonik", streamUrl = "https://radiorecord.hostingradio.ru/tecktonik_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Darkside DNB", streamUrl = "https://radiorecord.hostingradio.ru/darkside_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Breaks", streamUrl = "https://radiorecord.hostingradio.ru/breaks_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Future House", streamUrl = "https://radiorecord.hostingradio.ru/futurehouse_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Megabeat", streamUrl = "https://radiorecord.hostingradio.ru/megabeat_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Jungle", streamUrl = "https://radiorecord.hostingradio.ru/jungle_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Eurodance", streamUrl = "https://radiorecord.hostingradio.ru/eurodance_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Uplifting", streamUrl = "https://radiorecord.hostingradio.ru/uplifting_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Bass House", streamUrl = "https://radiorecord.hostingradio.ru/bass_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Progressive", streamUrl = "https://radiorecord.hostingradio.ru/progr_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Classic House", streamUrl = "https://radiorecord.hostingradio.ru/house_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Lo-Fi Hip Hop", streamUrl = "https://radiorecord.hostingradio.ru/lofi_128.mp3", genre = "Релакс"),
                RadioStation(name = "Record Ambient Chill", streamUrl = "https://radiorecord.hostingradio.ru/ambient_128.mp3", genre = "Релакс"),
                RadioStation(name = "Record Chillout Lounge", streamUrl = "https://radiorecord.hostingradio.ru/chil_128.mp3", genre = "Релакс"),
                RadioStation(name = "Record Moombahton", streamUrl = "https://radiorecord.hostingradio.ru/moombahton_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Neurofunk", streamUrl = "https://radiorecord.hostingradio.ru/neurofunk_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Dream Trance", streamUrl = "https://radiorecord.hostingradio.ru/dream_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record G-House", streamUrl = "https://radiorecord.hostingradio.ru/ghouse_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Tech House", streamUrl = "https://radiorecord.hostingradio.ru/techhouse_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Hardcore", streamUrl = "https://radiorecord.hostingradio.ru/hardcore_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Symphonic EDM", streamUrl = "https://radiorecord.hostingradio.ru/symph_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Complextro", streamUrl = "https://radiorecord.hostingradio.ru/complextro_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Psytrance", streamUrl = "https://radiorecord.hostingradio.ru/psy_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Dub Reggae", streamUrl = "https://radiorecord.hostingradio.ru/dub_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record French House", streamUrl = "https://radiorecord.hostingradio.ru/frenchhouse_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Goa Trance", streamUrl = "https://radiorecord.hostingradio.ru/goa_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Vocal House", streamUrl = "https://radiorecord.hostingradio.ru/vocalhouse_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record 2Step Garage", streamUrl = "https://radiorecord.hostingradio.ru/twostep_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Electro House", streamUrl = "https://radiorecord.hostingradio.ru/electrohouse_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Big Room", streamUrl = "https://radiorecord.hostingradio.ru/bigroom_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Dance Hits", streamUrl = "https://radiorecord.hostingradio.ru/dancehits_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Club Hits", streamUrl = "https://radiorecord.hostingradio.ru/clubhits_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Remixes", streamUrl = "https://radiorecord.hostingradio.ru/remix_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Old School Hits", streamUrl = "https://radiorecord.hostingradio.ru/oldschool_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Superdiskoteka", streamUrl = "https://radiorecord.hostingradio.ru/sd_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Rave FM", streamUrl = "https://radiorecord.hostingradio.ru/ravefm_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Russian Hits", streamUrl = "https://radiorecord.hostingradio.ru/russianhits_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Black Rap", streamUrl = "https://radiorecord.hostingradio.ru/black_128.mp3", genre = "Рэп / Хип-Хоп"),
                RadioStation(name = "Record Reggae Beats", streamUrl = "https://radiorecord.hostingradio.ru/reggae_128.mp3", genre = "Регги"),
                RadioStation(name = "Record Sax House", streamUrl = "https://radiorecord.hostingradio.ru/sax_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Piano House", streamUrl = "https://radiorecord.hostingradio.ru/piano_128.mp3", genre = "Клубная"),
                RadioStation(name = "Record Synth Pop", streamUrl = "https://radiorecord.hostingradio.ru/synth_128.mp3", genre = "Клубная")
            )

            for (station in defaults) {
                radioDao.insertStation(station)
            }
        }
    }
}
