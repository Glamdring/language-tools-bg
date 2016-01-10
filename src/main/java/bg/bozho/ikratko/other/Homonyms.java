package bg.bozho.ikratko.other;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.trie.PatriciaTrie;

import bg.bozho.ikratko.Checker;
import bg.bozho.ikratko.Checker.InflectedFormType;

import com.google.appengine.repackaged.com.google.common.collect.Maps;
import com.google.appengine.repackaged.com.google.common.collect.Sets;
import com.google.common.collect.Multimap;

/**
 * Намиране на всички омоними, вкл. тези, които са омоними само в определена форма.
 *
 * @author bozhanov
 *
 */
public class Homonyms {

    public static void main(String[] args) {
        Checker c = new Checker();
        c.initialize(false);
        Checker.formsDictionary.clear(); // we don't need it in memory
        
        Set<String> homonyms = Sets.newHashSet();
        
        // this is a variation of the formsDictionary initialization method, which counts homonyms
        PatriciaTrie<InflectedFormType> formsDictionary = new PatriciaTrie<InflectedFormType>();
        for (Map.Entry<String, Set<String>> word : Checker.dictionary.entrySet()) {
            String baseForm = word.getKey();
            if (word.getValue().isEmpty()) {
                checkAndInsertHomonym(baseForm, formsDictionary, homonyms, InflectedFormType.NOT_INFLECTABLE, baseForm);
                formsDictionary.put(baseForm, InflectedFormType.NOT_INFLECTABLE);
                continue;
            }
            for (String inflectionClass : word.getValue()) {
                Multimap<String, String> inflections = Checker.inflectionClasses.get(inflectionClass);
                if (inflections == null) {
                    checkAndInsertHomonym(baseForm, formsDictionary, homonyms, InflectedFormType.NOT_INFLECTABLE, baseForm);
                    formsDictionary.put(baseForm, InflectedFormType.NOT_INFLECTABLE);
                    continue;
                }

                if (inflectionClass.equals("O") && (baseForm.endsWith("ане") || baseForm.endsWith("яне"))) {
                    continue; // отлаголни съществителни
                }
                boolean specialCaseNoun = false;
                if (baseForm.endsWith("й") && (inflectionClass.equals("O") || inflectionClass.equals("M"))) {
                    specialCaseNoun = true;
                }
                boolean verb = Checker.verbClasses.contains(inflectionClass);

                for (String ending : inflections.keySet()) {
                    int endingIdx = baseForm.lastIndexOf(ending);
                    if (!baseForm.endsWith(ending) || endingIdx == -1) {
                        continue;
                    }
                    InflectedFormType type = Checker.getInflectedFormType(specialCaseNoun, verb, false);
                    checkAndInsertHomonym(baseForm, formsDictionary, homonyms, type, baseForm);
                    formsDictionary.put(baseForm, type);

                    Collection<String> pluralSuffixes = Checker.pluralInflectionClasses.get(inflectionClass).get(ending);
                    for (String suffix : inflections.get(ending)) {
                        String inflectedWord = baseForm.substring(0, endingIdx) + suffix;
                        boolean isPlural = pluralSuffixes.contains(suffix);
                        InflectedFormType formType =  Checker.getInflectedFormType(specialCaseNoun, verb, isPlural);
                        checkAndInsertHomonym(inflectedWord, formsDictionary, homonyms, formType, baseForm);
                        formsDictionary.put(inflectedWord, formType);
                    }
                }
            }
        }
        
        System.out.println(homonyms.size());
    }

    private static Map<String, String> mapping = Maps.newHashMap();
    
    private static void checkAndInsertHomonym(String word, PatriciaTrie<InflectedFormType> formsDictionary,
            Set<String> homonyms, InflectedFormType type, String baseForm) {
        if (formsDictionary.containsKey(word)) {
            String originalBase = mapping.get(word);
            InflectedFormType originalType = formsDictionary.get(word);
            
            if (baseForm != null && originalBase != null) {
                if (sameRoot(baseForm, originalBase) // heuristic based on length
                     || ignore(baseForm, originalBase, "н", "м", 1, 1, type, originalType, false, true) // "шлифовам" и "шлифован", напр.
                     || ignore(baseForm, originalBase, "я", "ен", 1, 2, type, originalType, true, false) // червя и червен
                     || ignore(baseForm, originalBase, "ващ", "вам", 1, 1, type, originalType, false, true)
                     || ignore(baseForm, originalBase, "вяне", "вям", 2, 1, type, originalType, false, true)
                     || ignore(baseForm, originalBase, "ан", "а", 1, 0, type, originalType, false, true)
                     || ignore(baseForm, originalBase, "ян", "а", 1, 0, type, originalType, false, true)
                     || ignore(baseForm, originalBase, "ение", "а", 4, 1, type, originalType, false, true)
                     || ignore(baseForm, originalBase, "я", "ение", 1, 4, type, originalType, true, false)
                     || ignore(baseForm, originalBase, "ат", "а", 1, 0, type, originalType, false, true)
                     || ignore(baseForm, originalBase, "ая", "ан", 1, 1, type, originalType, true, false)
                     || ignore(baseForm, originalBase, "я", "ене", 1, 3, type, originalType, true, false)
                     || ignore(baseForm, originalBase, "я", "ан", 1, 2, type, originalType, true, false)
                     || ignore(baseForm, originalBase, "ен", "а", 2, 1, type, originalType, false, true)
                     || ignore(baseForm, originalBase, "я", "ея", 1, 2, type, originalType, true, true)
                     || ignore(baseForm, originalBase, "я", "ещ", 1, 2, type, originalType, true, false)
                     || ignore(baseForm, originalBase, "ящ", "я", 2, 1, type, originalType, false, true)
                     || ignore(baseForm, originalBase, "ещ", "а", 2, 1, type, originalType, false, true)
                     || ignore(baseForm, originalBase, "ин", "", 2, 0, type, originalType, false, false)
                     || ignore(baseForm, originalBase, "ия", "ил", 2, 2, type, originalType, true, false)
                     || ignore(baseForm, originalBase, "ия", "ит", 2, 2, type, originalType, true, false)
                     || ignore(baseForm, originalBase, "ял", "я", 1, 0, type, originalType, false, true)
                     || ignore(baseForm, originalBase, "ям", "я", 1, 0, type, originalType, false, true)) {
                    return;
                    // омоними в основна форма - няма нужда от всичките им форми
                } else if (baseForm.equals(originalBase) && !word.equals(baseForm)) {
                    return;
                }
            }
            System.out.println(word + " (" + type + "): " + originalType + " (base: " + baseForm + "), original base: " + mapping.get(word) + ")");
            homonyms.add(word);
        } else {
            mapping.put(word, baseForm);
        }
    }

    private static boolean sameRoot(String baseForm, String originalBase) {
        // with sufficient length, we can ignore suffixes and 
        return baseForm.length() > 6 && originalBase.length() > 6
                && baseForm.substring(0, 5).equals(originalBase.substring(0, 5));
    }
    
    private static boolean ignore(String baseForm, String originalBase,
            String suffixBase, String suffixOriginal,
            int comparisonCutBase, int comparisonCutOriginal, 
            InflectedFormType type, InflectedFormType originalType, 
            boolean baseFormShouldBeVerb, boolean originalShouldBeVerb) {
        
        boolean formComparison = originalBase.endsWith(suffixOriginal) && baseForm.endsWith(suffixBase)
            && originalBase.substring(0, originalBase.length() - comparisonCutOriginal)
            .equals(baseForm.substring(0, baseForm.length() - comparisonCutBase));
        
        if (!formComparison) {
            return false;
        } else {
            // requirements for form types that would mean that the even though form comparison succeeds, the root is not the same
            boolean requirementsMet = false;
            if (!baseFormShouldBeVerb && !originalShouldBeVerb) {
                requirementsMet = true;
            }
            if (baseFormShouldBeVerb && type.isVerb()) {
                requirementsMet = true;
            }
            if (originalShouldBeVerb && originalType.isVerb()) {
                requirementsMet = true;
            }
            return requirementsMet;
        }
    }
}
