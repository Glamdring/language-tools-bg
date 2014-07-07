package bg.bozho.ikratko.other;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import bg.bozho.ikratko.Checker;

import com.google.common.collect.Sets;

@Service
public class ParonymService {

    public Set<String> findParonyms(String input) {
        Set<String> paronyms = Sets.newHashSet();
        int maxDistance = input.length() / 3;
        for (String word : Checker.formsDictionary.keySet()) {
            if (!word.startsWith(input) && StringUtils.getLevenshteinDistance(input, word, maxDistance) != -1) {
                paronyms.add(word);
            }
        }
        return paronyms;
    }
}
