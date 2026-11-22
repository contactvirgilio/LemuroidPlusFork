package com.swordfish.lemuroid.app.shared.game

/**
 * Tells Capcom CPS2 games apart from other arcade games by their ROM set name (FBNeo/MAME names),
 * so button mapping can show them as punches and kicks. Families include their regional sets and
 * clones (sfa3u, sfz3j, xmvsfu...).
 */
object Cps2Sets {
    private val prefixes =
        listOf(
            "ssf2", "sfa", "sfz", "spf2", "xmcota", "xmvsf", "mvsc", "msh", "vsav", "vhunt", "dstlk", "nwarr",
            "cybots", "sgemf", "pfght", "ringdest", "smbomb", "avsp", "ddtod", "ddsom", "19xx", "1944",
            "armwar", "pgear", "batcir", "gigawing", "mmatrix", "progear", "megaman2", "rockman2", "mmancp2",
            "rmancp2", "hsf2", "csclub", "cscl", "qndream", "choko", "dimahoo", "gmahou", "jyangoku", "mpang",
            "pzloop2", "ecofghtr", "uecology", "spf2t", "sfa2", "sfa3",
        )

    fun isCps2(romFileName: String): Boolean {
        val name = romFileName.substringBeforeLast('.').lowercase()
        return prefixes.any { name.startsWith(it) }
    }
}
