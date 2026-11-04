package com.swordfish.lemuroid.app.shared.game

/**
 * Capcom CPS1 and CPS2 games inside the Arcade library. They all draw a fixed 384 x 224 picture
 * (shown at 4:3), so like Neo Geo they can use whole-number scaling. Recognised by the name of the
 * ROM set; a game missing here keeps the normal "Fit" size.
 */
object CpsSets {
    /** Families of ROM sets, including regional versions and clones (sf2ua, sf2hf, ffightj...). */
    private val prefixes =
        listOf(
            // CPS1
            "sf2", "ssf2", "ffight", "ghouls", "knights", "kod", "mercs", "msword", "mtwins",
            "nemo", "pang3", "strider", "unsquad", "varth", "willow", "wof", "cworld2", "captcomm",
            "daimakai", "dynwar", "forgottn", "lostwrld", "cawing", "qad", "1941", "3wonders", "area88",
            "pnickj", "megaman", "rockmanj", "cadillac", "punisher", "slammast", "pokonyan",
            "sfzch", "fcrash", "captcom",
            // CPS2
            "1944", "19xx", "ddtod", "ddsom", "dstlk", "vampj", "vamp", "msh", "mshvsf", "mvsc", "xmcota",
            "xmvsf", "sfa", "sfz", "spf2", "hsf2", "cybots", "dimahoo", "gigawing", "progear", "batcir",
            "choko", "csclub", "ecofghtr", "jyangoku", "mpang", "nwarr", "pzloop2", "qndream", "rockman2j",
            "sgemf", "vhunt2", "vsav", "armwar", "avsp", "ringdest", "smbomb", "ssf2t", "gmahou",
            "mmancp2u", "megaman2", "dstlku", "darkstal", "hsf2j", "sfa2", "sfa3", "ssf2x",
        )

    // Names too close to other arcade games to match by their start (dinorex is a Taito game).
    private val exact = setOf("dino", "dinoj", "dinou", "dinoh", "dinoa", "dinot")

    fun isCps(romFileName: String): Boolean {
        val name = romFileName.substringBeforeLast('.').lowercase()
        return name in exact || prefixes.any { name.startsWith(it) }
    }
}
