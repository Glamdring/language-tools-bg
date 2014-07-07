package bg.bozho.ikratko;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.google.common.collect.Sets;

public class NumeralDetector {
    // only masculine 2, because other genders are not relevant to й
    private static final Set<String> baseNumerals = Sets.newHashSet("два", "три", "четири", "пет", "шест", "седем", "осем", "девет" , "десет");

    public static boolean isNumeral(String word) {
        if (StringUtils.isEmpty(word)) {
            return false;
        }

        if (word.endsWith("-ма")) {
            word = word.replace("-ма", "");
        }
        if (NumberUtils.isNumber(word)) {
            return true;
        }
        if (baseNumerals.contains(word)) {
            return true;
        }

        for (String baseNumeral : baseNumerals) {
            // трима, четиримата, трийсет, петдесет, седемстотин
            if (word.startsWith(baseNumeral) && (
                    word.endsWith("ма")
                    || word.endsWith("мата")
                    || word.endsWith("надесет")
                    || word.endsWith("найсет")
                    || word.endsWith("стотин")
                    || word.endsWith("десет")
                    || word.endsWith("йсет"))) {
                return true;
            }
            // exceptions
            if (word.equals("седмина") || word.equals("осмина") || word.equals("двеста")
                    || word.equals("триста") || word.equals("сто")) {
                return true;
            }
        }

        return false;
    }
}
