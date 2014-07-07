package bg.bozho.ikratko.other;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.ardverk.collection.PatriciaTrie;
import org.ardverk.collection.StringKeyAnalyzer;
import org.ardverk.collection.Trie;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;


import bg.bozho.ikratko.Checker;
import bg.bozho.ikratko.Checker.InflectedFormType;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@Service
@DependsOn("checker")
public class RhymeService {

    private Trie<String, List<String>> reverse;
    private static char[] vowels = new char[] {'а', 'ъ', 'о', 'у', 'е', 'и', 'ю', 'я'};

    @PostConstruct
    public void init() {
        reverse = new PatriciaTrie<String, List<String>>(StringKeyAnalyzer.CHAR);
        for (Entry<String, Checker.InflectedFormType> entry: Checker.formsDictionary.entrySet()) {
            //using a StringBuilder so that no entry is placed in the jvm string pool
            String key = new StringBuilder(entry.getKey()).reverse().substring(0, Math.min(5, entry.getKey().length()));
            List<String> list = reverse.get(key);
            if (list == null) {
                list = new ArrayList<String>();
                reverse.put(key, list);
            }
            list.add(entry.getKey());
        }
    }

    public Set<String> getRhymes(String ending, int syllables) {
        SortedMap<String, List<String>> reverseRhymes = reverse.prefixMap(StringUtils.reverse(ending));
        Set<String> rhymes = Sets.newHashSetWithExpectedSize(reverseRhymes.size());
        for (Entry<String, List<String>> reverseRhyme : reverseRhymes.entrySet()) {
            for (String word : reverseRhyme.getValue()) {
                // optionally limit the result to words with certain length
                if (syllables > 0 && getSyllables(word) != syllables) {
                    continue;
                }
                rhymes.add(word);
            }
        }

        return rhymes;
    }

    public static void main(String[] args) {
        System.out.println(new RhymeService().getSyllables("лайно"));
    }
    private int getSyllables(String word) {
        int syllables = 0;
        for (int i = 0; i < word.length(); i++) {
            if (Arrays.binarySearch(vowels, word.charAt(i)) > -1) {
                syllables++;
            }
        }
        return syllables;
    }


    private static Map<Character, Character> similarReplacements = Maps.newHashMap();
    static {
        Arrays.sort(vowels);
        similarReplacements.put('м', 'н'); similarReplacements.put('н', 'м');
        similarReplacements.put('т', 'д'); similarReplacements.put('д', 'т');
        similarReplacements.put('п', 'б'); similarReplacements.put('б', 'п');
        similarReplacements.put('к', 'г'); similarReplacements.put('г', 'к');
        similarReplacements.put('ш', 'ж'); similarReplacements.put('ж', 'ш');
        similarReplacements.put('с', 'з'); similarReplacements.put('з', 'с');
        similarReplacements.put('с', 'ш'); similarReplacements.put('ш', 'с');
        similarReplacements.put('ж', 'з'); similarReplacements.put('з', 'ж');
        similarReplacements.put('ф', 'в'); similarReplacements.put('в', 'ф');
    }

    public Set<String> getSimilarEndings(String ending) {
        Set<String> similar = Sets.newHashSet();
        for (int i = 0; i < ending.length(); i++) {
            Character replacement = similarReplacements.get(ending.charAt(i));
            if (replacement != null) {
                similar.add(ending.replace(ending.charAt(i), replacement));
            }
        }
        return similar;
    }
}
