package com.swordfish.lemuroid.app.shared.game

/**
 * Capcom CPS1, CPS2 and CPS3 games inside the Arcade library, recognised by the name of the ROM
 * set (the zip file name). CPS1 and CPS2 draw a fixed 384 x 224 picture (shown at 4:3), so like
 * Neo Geo they can use whole-number scaling. A game missing here is treated as a regular arcade
 * game.
 */
object CpsSets {
    private val cps3 = listOf("sfiii", "jojo", "redearth", "warzard")

    // Checked before the CPS2 families: "sfzch" (CPS Changer) would otherwise match "sfz".
    private val cps1Exact = setOf("sfzch", "sfzcha", "sfzchj", "dino", "dinoj", "dinou", "dinoh", "dinoa", "dinot")

    private val cps2 =
        listOf(
            "1944", "19xx", "ddtod", "ddsom", "dstlk", "vampj", "vamp", "msh", "mshvsf", "mvsc", "xmcota",
            "xmvsf", "sfa", "sfz", "spf2", "hsf2", "cybots", "dimahoo", "gigawing", "progear", "batcir",
            "choko", "csclub", "ecofghtr", "jyangoku", "mpang", "nwarr", "pzloop2", "qndream", "rockman2j",
            "sgemf", "vhunt2", "vsav", "armwar", "avsp", "ringdest", "smbomb", "ssf2", "gmahou",
            "mmancp2u", "megaman", "rockmanj", "darkstal", "sfa2", "sfa3",
        )

    private val cps1 =
        listOf(
            "sf2", "ffight", "ghouls", "knights", "kod", "mercs", "msword", "mtwins",
            "nemo", "pang3", "strider", "unsquad", "varth", "willow", "wof", "cworld2", "captcomm",
            "daimakai", "dynwar", "forgottn", "lostwrld", "cawing", "qad", "1941", "3wonders", "area88",
            "pnickj", "cadillac", "punisher", "slammast", "pokonyan", "fcrash", "captcom",
        )

    /** 1, 2 or 3 for a Capcom CPS game, null for anything else. */
    fun generation(romFileName: String): Int? {
        val name = romFileName.substringBeforeLast('.').lowercase()
        return when {
            cps3.any { name.startsWith(it) } -> 3
            name in cps1Exact -> 1
            cps2.any { name.startsWith(it) } -> 2
            cps1.any { name.startsWith(it) } -> 1
            else -> null
        }
    }

    /** CPS1 and CPS2 games (fixed 384 x 224 picture). */
    fun isCps(romFileName: String): Boolean = generation(romFileName).let { it == 1 || it == 2 }
}
