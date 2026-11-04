package com.swordfish.lemuroid.app.shared.game

/**
 * Neo Geo games inside the Arcade (FBNeo) library. Every one of them draws the same 320 x 224
 * picture, so unlike the rest of Arcade they can use whole-number scaling. Recognised by the name
 * of the ROM set (the zip file name); a game missing here just keeps the normal "Fit" size.
 */
object NeoGeoSets {
    /** Whole families of ROM sets, including their clones and bootlegs (kof98k, mslug3h...). */
    private val prefixes =
        listOf(
            "kof", "kf2k", "kf10", "mslug", "ms4plus", "ms5plus", "samsho", "samsh5", "garou", "fatfur",
            "rbff", "aof", "svc", "matrim", "sonicwi", "ssideki", "sengoku", "lastbl", "wh1", "wh2",
        )

    private val exact: Set<String> =
        setOf(
            "nam1975", "bstars", "bstars2", "lresort", "tpgolf", "socbrawl", "joyjoy", "ridhero",
            "ridheroh", "maglord", "maglordh", "burningf", "ncombat", "ncombath", "cyberlip", "3countb",
            "eightman", "ragnagrd", "pgoal", "magdrop2", "magdrop3", "mahretsu", "pulstar", "viewpoint",
            "spinmast", "gpilots", "gpilotsh", "trally", "turfmast", "twinspri", "zedblade", "zupapa",
            "janshin", "bjourney", "crsword", "crsword2", "doubledr", "flipshot", "fightfev", "gowcaizr",
            "goalx3", "galaxyfg", "karnovr", "kizuna", "legendos", "lbowling", "minasan", "neobombe",
            "neocup98", "neodrift", "neomrdo", "ninjamas", "panicbom", "puzzledp", "puzzldpr", "quizdais",
            "quizdai2", "quizkof", "rotd", "savagere", "stakwin", "stakwin2", "strhoop", "superspy",
            "tophuntr", "tws96", "aodk", "androdun", "alpham2", "breakers", "breakrev", "b2b", "bangbead",
            "shocktro", "shocktr2", "kotm", "kotm2", "mosyougi", "marukodq", "dragonsh", "preisle2",
            "diggerma", "fswords", "ironclad", "irrmaze", "popbounc", "roboarmy", "sdodgeb", "vliner",
            "wakuwak7", "ct2k3sp", "ct2k3sa", "lans2004", "pnyaa", "ghostlop", "bakatono", "mutnat",
            "nitd", "adkworld", "blazstar", "whp", "wjammers", "s1945p", "pbobblen", "pbobbl2n",
            "bangbedp", "ssideki", "tws96", "overtop", "wh1", "wh2", "wh2j",
        )

    fun isNeoGeo(romFileName: String): Boolean {
        val name = romFileName.substringBeforeLast('.').lowercase()
        return name in exact || prefixes.any { name.startsWith(it) }
    }
}
