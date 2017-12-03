package bg.bozho.ikratko.other;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.iterators.PermutationIterator;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import bg.bozho.ikratko.Checker;

@Service
public class Anagram {

    public Set<String> getAnagrams(String word) {
        //TODO n-grams
        Set<String> anagrams = new HashSet<String>();
        char[] chars = word.toLowerCase().toCharArray();
        List<Character> list = Arrays.asList(ArrayUtils.toObject(chars));
        PermutationIterator<Character> gen = new PermutationIterator<Character>(list);
		StringBuilder sb = new StringBuilder();
        while (gen.hasNext()) {
            List<Character> anagramList = gen.next();
            
            for (Character ch : anagramList) {
                sb.append(ch);
            }
            String anagram = sb.toString();
            if (Checker.formsDictionary.containsKey(anagram)) {
                anagrams.add(anagram);
            }
			sb.setLength(0);
        }
        anagrams.remove(word.toLowerCase()); // don't return the input word
        return anagrams;
    }

    public static void main(String[] args) throws Exception {
        int max = 0;
        String maxWord = "";
        Checker ch = new Checker();
        ch.initialize();

        for (String form : ch.formsDictionary.keySet()) {
            int value = calculateMorz(form);
            if (value > 160) {
                System.out.println(form + "=" + value);
            }
            if (value > max) {
                maxWord = form;
                max = value;
            }
        }
        System.out.println(max);
        System.out.println(maxWord);
    }

    private static final Map<Character, Integer> weights = Maps.newHashMap();
    static {
        weights.put('а', 4); weights.put('б', 6); weights.put('в', 7);
        weights.put('г', 7); weights.put('д', 5); weights.put('е', 1);
        weights.put('ж', 6); weights.put('з', 8); weights.put('и', 2);
        weights.put('й', 10); weights.put('к', 7); weights.put('л', 6);
        weights.put('м', 6); weights.put('н', 4); weights.put('о', 9);
        weights.put('п', 8); weights.put('р', 5); weights.put('с', 3);
        weights.put('т', 3); weights.put('у', 5); weights.put('ф', 6);
        weights.put('х', 4); weights.put('ц', 8); weights.put('ч', 10);
        weights.put('ш', 12); weights.put('щ', 10); weights.put('ъ', 8);
        weights.put('ь', 10); weights.put('ю', 8); weights.put('я', 8);
        weights.put('ѝ', 2);
    }

    private static int calculateMorz(String form) {
        int sum = 0;
        for (char chr : form.toCharArray()) {
            Integer w = weights.get(Character.toLowerCase(chr));
            if (w != null) {
                sum += w;
            } else {
                System.out.println(chr);
            }
        }
        sum += form.length() - 1;
        return sum;
    }
}
