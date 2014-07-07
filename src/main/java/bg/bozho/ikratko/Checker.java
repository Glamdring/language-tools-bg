package bg.bozho.ikratko;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.ardverk.collection.PatriciaTrie;
import org.ardverk.collection.StringKeyAnalyzer;
import org.ardverk.collection.Trie;
import org.springframework.stereotype.Component;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

@Component
public class Checker {

    public static Trie<String, Set<String>> dictionary;
    public static Trie<String, InflectedFormType> formsDictionary;

    private static boolean initialized = false;

    private static Map<String, Multimap<String, String>> inflectionClasses = Maps.newHashMap();
    private static Map<String, Multimap<String, String>> pluralInflectionClasses = Maps.newHashMap();
    private static final String POTENTIAL_MISTAKE_REGEX = "(\\p{L}*[аъоуеиюя][ий]\\p{L}*)";
    private static final String POTENTIAL_MISTAKE_REGEX_I = "(\\p{L}*[аъоуеиюя])и(\\p{L}*)";
    private static final String POTENTIAL_MISTAKE_REGEX_Y = "(\\p{L}*[аъоуеиюя])й(\\p{L}*)";

    private static final String END_OF_SENTENCE = "[\\.!?]";

    private static final List<String> toBeFormsSg = Arrays.asList("съм", "си", "е", "бях", "беше", "бъда", "бъдеш", "бъде");
    private static final List<String> toBeFormsPl = Arrays.asList("сме", "сте", "са", "бяхме", "бяхте", "бяха", "бъдат");

    private static final Set<String> pronounsSgSet = Sets.newHashSet("някой", "никой", "кой", "чий");
    private static final Set<String> pronounsPlSet = Sets.newHashSet("някои", "никои", "кои", "чии");
    private static final Set<String> linkingPronounsSgSet = Sets.newHashSet("който", "чийто");
    private static final Set<String> linkingPronounsPlSet = Sets.newHashSet("които", "чиито");

    private static final Set<String> pluralIdentfiers = Sets.newHashSet("няколко", "николко", "много", "малко", "доста", "брой", "безброй", "тези", "онези");
    private static final Set<String> singularIdentfiers = Sets.newHashSet("един", "този", "онзи");
    private static final Set<String> verbClasses = Sets.newHashSet("P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z");

    @PostConstruct
    public synchronized void initialize() {
        if (!initialized) {
            load();
            loadInflections();
            loadFormsDictionary();
            initialized = true;
        }
    }

    public Result process(String input, boolean spellcheckAll) {
        if (!initialized) {
            throw new IllegalStateException("The checker must be initialized first");
        }
        // getting all words + the punctuation marks for end of sentence
        // in order to be able to identify sentence boundaries
        String[] words = input.split("(?<=" + END_OF_SENTENCE + ")|(?=" + END_OF_SENTENCE + ")|(\\p{Punct}*\\p{Space}+)");

        for (int i = 2; i < words.length; i ++) {
            if (words[i].equals("ма") && NumberUtils.isNumber(words[i-1])) {
                words[i-1] = words[i-1] + "-ма";
                words = ArrayUtils.remove(words, i);
            }
        }

        System.out.println(StringUtils.join(words, "|"));
        List<Mistake> potentialMistakes = new ArrayList<Mistake>();
        List<Mistake> mistakes = new ArrayList<Mistake>();
        List<Mistake> otherMistakes = new ArrayList<Mistake>();
        List<String> properNames = new ArrayList<String>();

        int idx = 0;
        int lengthSum = 0;
        for (String word : words) {
            if (StringUtils.isEmpty(word)) {
                continue;
            }
            // proper names are not checked. They are those starting with
            // capital letter and are not in the beginning of a sentence
            int previousWordIdx = idx - 1;
            if (previousWordIdx >= 0) {
                if (Character.isUpperCase(word.charAt(0)) && !words[previousWordIdx].matches(END_OF_SENTENCE)) {
                    properNames.add(word);
                    continue;
                }
            }

            if (word.matches(POTENTIAL_MISTAKE_REGEX)) {
                Mistake pm = new Mistake();
                pm.setWord(word.toLowerCase());
                // set as next (and previous) only words that can be inflected. If the next word
                // is a misspelled one (not found in the dictionary), set it as empty
                int nextWordIdx = idx + 1;
                while (words.length > nextWordIdx && nextWordIdx - idx < 5) {
                    String nextWord = words[nextWordIdx++].toLowerCase();
                    if (nextWord.matches(END_OF_SENTENCE)) {
                        break;
                    }
                    InflectedFormType inflectedFormType = formsDictionary.get(nextWord);
                    if (inflectedFormType == null || inflectedFormType != InflectedFormType.NOT_INFLECTABLE) {
                        pm.setNextInflectableWord(StringUtils.trimToEmpty(nextWord));
                        break;
                    }
                }
                while (previousWordIdx > -1 && idx - previousWordIdx < 5) {
                    String previousWord = words[previousWordIdx].toLowerCase();
                    if (previousWord.matches(END_OF_SENTENCE)) {
                        break;
                    }
                    if (previousWordIdx == idx - 1) {
                        pm.setPreviousWord(previousWord);
                    }

                    InflectedFormType inflectedFormType = formsDictionary.get(previousWord);
                    if (inflectedFormType != null && inflectedFormType != InflectedFormType.NOT_INFLECTABLE) {
                        pm.setPreviousInflectableWord(StringUtils.trimToEmpty(previousWord));
                        break;
                    }
                    if ((inflectedFormType == null || inflectedFormType == InflectedFormType.NOT_INFLECTABLE)
                            && NumeralDetector.isNumeral(previousWord)) {
                        pm.setPreviousInflectableWord(previousWord);
                    }
                    previousWordIdx--;
                }
                pm.setIndexInText(input.indexOf(word, lengthSum));
                potentialMistakes.add(pm);
            } else if (spellcheckAll) {
                if (formsDictionary.get(word) == null) {
                    otherMistakes.add(new Mistake(word));
                }
            }
            idx++;
            lengthSum += word.length();
        }

        System.out.println(potentialMistakes);

        for (Iterator<Mistake> it = potentialMistakes.iterator(); it.hasNext();) {
            Mistake potentialMistake = it.next();

            // if this word is an inexistent word form, but its alternative in terms of и/й exists -
            // it's a и/й mistake, otherwise - it's a regular spelling mistake
            if (!isExistingWordForm(potentialMistake.getWord())) {
                String alternative = null;
                if (potentialMistake.getWord().matches(POTENTIAL_MISTAKE_REGEX_I)) {
                    alternative = potentialMistake.getWord().replaceAll(POTENTIAL_MISTAKE_REGEX_I, "$1й$2");
                } else if (potentialMistake.getWord().matches(POTENTIAL_MISTAKE_REGEX_Y)) {
                    alternative = potentialMistake.getWord().replaceAll(POTENTIAL_MISTAKE_REGEX_Y, "$1и$2");
                }
                if (!isExistingWordForm(alternative)) {
                    otherMistakes.add(potentialMistake);
                } else {
                    mistakes.add(potentialMistake);
                }
                continue;
            }

            handleWrongLinkingPronounForm(potentialMistake, mistakes);
            handleWrongPronounForm(potentialMistake, mistakes);
            handleWrongPluralForms(potentialMistake, mistakes);
            //handle wrong imperatives: пеЙ, пеи
        }

        Result result = new Result();
        result.setMistakes(mistakes);
        result.setOtherSpellingMistakes(otherMistakes);
        result.setProperNames(properNames);

        return result;
    }

    private boolean isExistingWordForm(String word) {
        if (!formsDictionary.containsKey(word)) {
            return false;
        }

        return true;
    }

    private void handleWrongPronounForm(Mistake potentialMistake, List<Mistake> mistakes) {
        //"някой", "никой", "кой"
        if (potentialMistake.matches("няко[ий]|нико[ий]|ко[ий]")) {
            if (!agreesOnPlurality(potentialMistake.getWord(), potentialMistake.getNextInflectableWord(),
                    pronounsSgSet, pronounsPlSet)) {
                mistakes.add(potentialMistake);
            }
        }
    }

    private void handleWrongLinkingPronounForm(Mistake potentialMistake, List<Mistake> mistakes) {
        if (potentialMistake.matches("чи[ий]то") ) {
            if (!agreesOnPlurality(potentialMistake.getWord(), potentialMistake.getNextInflectableWord(),
                    linkingPronounsSgSet, linkingPronounsPlSet)) {
                mistakes.add(potentialMistake);
            }
        }
        if (potentialMistake.matches("ко[ий]то|") ) {
            boolean isNextWordVerb = false;
            if (StringUtils.isNotEmpty(potentialMistake.getNextInflectableWord())) {
                InflectedFormType nextWordFormType = formsDictionary.get(potentialMistake.getNextInflectableWord());
                isNextWordVerb = nextWordFormType != null && nextWordFormType.isVerb();
            }
            // if the next word is a verb, check for agreement with the previous (the verb does not necessarily agree with the pronoun)
            // if there is no previous word (i.e. if it's the start of the sentence), use the next word
            String agreeingWord = StringUtils.isNotEmpty(potentialMistake.getPreviousInflectableWord()) ? potentialMistake
                    .getPreviousInflectableWord() : potentialMistake.getNextInflectableWord();

            if (isNextWordVerb && !agreesOnPlurality(potentialMistake.getWord(), agreeingWord,
                    linkingPronounsSgSet, linkingPronounsPlSet)) {
                mistakes.add(potentialMistake);
            } else if (!isNextWordVerb && !agreesOnPlurality(potentialMistake.getWord(), potentialMistake.getNextInflectableWord(),
                    linkingPronounsSgSet, linkingPronounsPlSet)) {
                mistakes.add(potentialMistake);
            }
        }
    }

    private void handleWrongPluralForms(Mistake potentialMistake, List<Mistake> mistakes) {
        // cases like полицай/полицаи, трамвай/трамваи
        InflectedFormType formType = formsDictionary.get(potentialMistake.getWord());
        if (formType.isSpecialCaseNoun()) {
            InflectedFormType previousFormType = formsDictionary.get(potentialMistake.getPreviousWord());
            if (previousFormType == InflectedFormType.NOT_INFLECTABLE || previousFormType == null) {
                if (!formType.isPlural() && NumeralDetector.isNumeral(potentialMistake.getPreviousInflectableWord())) {
                    mistakes.add(potentialMistake);
                } else if (pluralIdentfiers.contains(potentialMistake.getPreviousWord()) || NumeralDetector.isNumeral(potentialMistake.getPreviousWord())) {
                    if (!formType.isPlural()) {
                        mistakes.add(potentialMistake);
                    }
                } else if (singularIdentfiers.contains(potentialMistake.getPreviousWord())) {
                    if (formType.isPlural()) {
                        mistakes.add(potentialMistake);
                    }
                } else {
                    InflectedFormType previousInflectedWordFormType = formsDictionary.get(potentialMistake.getPreviousInflectableWord());
                    if (previousInflectedWordFormType != null && disagreesOnPlurality(formType, previousInflectedWordFormType)) {
                        mistakes.add(potentialMistake);
                    }
                }
            } else if (previousFormType != null) {
                if (disagreesOnPlurality(formType, previousFormType)) {
                    mistakes.add(potentialMistake);
                }
            }
        }
    }

    private boolean disagreesOnPlurality(InflectedFormType formType, InflectedFormType previousFormType) {
        // if they don't agree on plurality, it's a mistake
        return BooleanUtils.xor(new boolean[] {previousFormType.isPlural(), formType.isPlural()});
    }


    @Deprecated
    private boolean handleWrongPluralSimple(Mistake potentialMistake, List<Mistake> mistakes) {
        if (potentialMistake.getWord().endsWith("й")) {
            String baseForm = potentialMistake.getWord().substring(0, potentialMistake.getWord().length() - 1) + "я";
            for (String type : dictionary.get(baseForm)) {
                if (type != null && type.equals("M") || type.equals("K")) {
                    mistakes.add(potentialMistake);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean agreesOnPlurality(String word, String agreeingWord, Set<String> singularWords, Set<String> pluralWord) {
        // if there is nothing to agree with assume the form is correct
        if (StringUtils.isEmpty(agreeingWord)) {
            return true;
        }

        InflectedFormType formType = formsDictionary.get(agreeingWord);
        if (formType == null) {
            return false;
        }

        if ((singularWords.contains(word) && !formType.isPlural())
                || (pluralWord.contains(word) && formType.isPlural())) {
            return true;
        }
        return false;
    }

    public static void load() {
        InputStream is = Checker.class.getResourceAsStream("/bg_BG.dic");
        List<String> lines = null;

        try {
            lines = IOUtils.readLines(is, "utf-8");
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        } finally {
            IOUtils.closeQuietly(is);
        }

        dictionary = new PatriciaTrie<String, Set<String>>(StringKeyAnalyzer.CHAR);
        for (String line : lines) {
            int paradigmIdx = line.indexOf("/");
            if (paradigmIdx != -1) {
                String inflectionClasses = line.substring(paradigmIdx + 1);
                // /AK is possible, i.e. multiple infl. classes per word
                dictionary.put(line.substring(0, paradigmIdx).toLowerCase(),
                        Sets.newHashSet(charToStringArray(inflectionClasses.toCharArray())));
            } else {
                dictionary.put(line.toLowerCase(), Collections.<String>emptySet());
            }
        }
    }

    public static void loadFormsDictionary() {
        formsDictionary = new PatriciaTrie<String, InflectedFormType>(StringKeyAnalyzer.CHAR);
        for (Map.Entry<String, Set<String>> word : dictionary.entrySet()) {
            String baseForm = word.getKey();
            if (word.getValue().isEmpty()) {
                formsDictionary.put(baseForm, InflectedFormType.NOT_INFLECTABLE);
                continue;
            }
            for (String inflectionClass : word.getValue()) {
                Multimap<String, String> inflections = inflectionClasses.get(inflectionClass);
                if (inflections == null) {
                    formsDictionary.put(baseForm, InflectedFormType.NOT_INFLECTABLE);
                    continue;
                }

                boolean specialCaseNoun = false;
                if (baseForm.endsWith("й") && (inflectionClass.equals("O") || inflectionClass.equals("M"))) {
                    specialCaseNoun = true;
                }
                boolean verb = verbClasses.contains(inflectionClass);

                for (String ending : inflections.keySet()) {
                    int endingIdx = baseForm.lastIndexOf(ending);
                    if (!baseForm.endsWith(ending) || endingIdx == -1) {
                        continue;
                    }
                    formsDictionary.put(baseForm, getInflectedFormType(specialCaseNoun, verb, false));

                    Collection<String> pluralSuffixes = pluralInflectionClasses.get(inflectionClass).get(ending);
                    for (String suffix : inflections.get(ending)) {
                        String inflectedWord = baseForm.substring(0, endingIdx) + suffix;
                        boolean isPlural = pluralSuffixes.contains(suffix);
                        formsDictionary.put(inflectedWord, getInflectedFormType(specialCaseNoun, verb, isPlural));
                    }
                }
            }
        }

        // override the forms of the verb "to be"
        for (String sgForm : toBeFormsSg) {
            formsDictionary.put(sgForm, InflectedFormType.REGULAR_FORM_VERB);
        }
        for (String plForm : toBeFormsPl) {
            formsDictionary.put(plForm, InflectedFormType.PLURAL_FORM_VERB);
        }

        dictionary = null; // eligible for GC. TODO can merge these two load methods, but it's easier not to, for now
    }

    private static InflectedFormType getInflectedFormType(boolean specialCaseNoun, boolean verb, boolean plural) {
       if (specialCaseNoun && plural) {
           return InflectedFormType.PLURAL_FORM_SPECIAL;
       } else if (specialCaseNoun && !plural) {
           return InflectedFormType.REGULAR_FORM_SPECIAL;
       } else if (verb && plural) {
           return InflectedFormType.PLURAL_FORM_VERB;
       } else if (verb && !plural) {
           return InflectedFormType.REGULAR_FORM_VERB;
       } else if (plural) {
           return InflectedFormType.PLURAL_FORM;
       } else {
           return InflectedFormType.REGULAR_FORM;
       }
    }

    public static void loadInflections() {
        InputStream inputStreamAll = Checker.class.getResourceAsStream("/bg_BG.aff");
        fillInflectionClasses(inflectionClasses, inputStreamAll);

        InputStream inputStreamPlurals = Checker.class.getResourceAsStream("/plurals.aff");
        fillInflectionClasses(pluralInflectionClasses, inputStreamPlurals);

    }

    private static void fillInflectionClasses(Map<String, Multimap<String, String>> map, InputStream is) {
        List<String> lines = null;
        try {
            lines = IOUtils.readLines(is, "utf-8");
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        } finally {
            IOUtils.closeQuietly(is);
        }

        boolean newInflectionClass = false;
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                newInflectionClass = true;
                continue;
            }
            if (!line.startsWith("SFX")) {
                continue;
            }

            String inflectionClass = line.substring(4, 5);
            if (newInflectionClass) {
                map.put(inflectionClass, HashMultimap.<String, String>create());
            } else {
                String[] parts = line.split("\\p{Space}+");
                String suffix = parts[3];
                if (suffix.equals("0")) {
                    suffix = "";
                }
                String baseFormEnding = parts[2];
                if (baseFormEnding.equals("0")) {
                    baseFormEnding = "";
                }
                // the inflection suffixes are the values of the multimap, with key=the base form ending
                map.get(inflectionClass).put(baseFormEnding, suffix);
            }
            newInflectionClass = false;
        }
    }

    private static String[] charToStringArray(char[] array) {
        String[] result = new String[array.length];
        for (int i = 0; i < array.length; i ++) {
            result[i] = String.valueOf(array[i]);
        }
        return result;
    }

    public static void main(String args[]) throws Exception {

        String input = "Който управлява и провежда изборите, невинаги ги печели, но почти винаги върши неща извън позволените, защото средствата за разкриването им са под негов контрол. Нарушения вършат и чуждите, но вероятността те да бъдат посочени, а нашите покрити, е докъм 100-процентова.";
        //input = "Децата, чийто обувки продадоха";
        //input = "Човекът, който пее";
        //input = "Кои ще дойде с мен";
        //input = "Човекът, които дойде, ще ни донесе филий";
        //input = "В София има много автобуси и трамваи.";
        //input = "Качих се на един трамвай";
        //input = "Качих 32-ма полицаи. Тъпи ченгета! Край.";
        //input = "Които пее, зло не мисли";
        //input = "видях бастуна, които те купиха";
        //input = "двамата полицай, който видях";
        //input = "хората, които дойдоха, ядат филий";
        //input = "трима полицай";
        //input = "Има ли хора, които да си хапват пълнени чушки точно в този момент?";
        input = "Господин полицаи, оставете ме намира.";
        input = "Двамата полицай, който видях";
        input = "някой";
        input = "Общината планува да закупи 10 тролей";
        input = "Някой, който идва";
        input = "пеней на гошо, гошовата майка";
        input = "кой";
        input = "баба й дядо";
        input = "Дойде Антиохий и падна зад мъжа, който те изядоха";
        Result result = new Checker().process(input, true);
        System.out.println(result.getMistakes());
        System.out.println(result.getOtherSpellingMistakes());
        System.out.println(result.getProperNames());
    }

    public static class Mistake {
        private String word;
        private String clause = "";
        private String nextInflectableWord = "";
        private String previousInflectableWord = "";
        private String previousWord = "";
        private int indexInText;

        public Mistake() {

        }
        public Mistake(String word) {
            this.word = word;
        }
        public String getWord() {
            return word;
        }
        public void setWord(String word) {
            this.word = word;
        }
        public String getClause() {
            return clause;
        }
        public void setClause(String sentence) {
            this.clause = sentence;
        }
        public String getNextInflectableWord() {
            return nextInflectableWord;
        }
        public void setNextInflectableWord(String nextWord) {
            this.nextInflectableWord = nextWord;
        }
        public int getIndexInText() {
            return indexInText;
        }
        public void setIndexInText(int indexInText) {
            this.indexInText = indexInText;
        }
        public String getPreviousInflectableWord() {
            return previousInflectableWord;
        }
        public void setPreviousInflectableWord(String previousWord) {
            this.previousInflectableWord = previousWord;
        }
        public String getPreviousWord() {
            return previousWord;
        }
        public void setPreviousWord(String previousWord) {
            this.previousWord = previousWord;
        }
        public boolean matches(String regex) {
            return word.matches(regex);
        }
        @Override
        public String toString() {
            return "Mistake [word=" + word + ", nextInflectableWord=" + nextInflectableWord
                    + ", previousInflectableWord=" + previousInflectableWord + ", previousWord="
                    + previousWord + "]";
        }
    }

    public static class Result {
        private List<Mistake> mistakes;
        private List<Mistake> otherSpellingMistakes;
        private List<String> properNames;

        public List<Mistake> getMistakes() {
            return mistakes;
        }
        public void setMistakes(List<Mistake> mistakes) {
            this.mistakes = mistakes;
        }
        public List<Mistake> getOtherSpellingMistakes() {
            return otherSpellingMistakes;
        }
        public void setOtherSpellingMistakes(List<Mistake> spellingMistakes) {
            this.otherSpellingMistakes = spellingMistakes;
        }
        public List<String> getProperNames() {
            return properNames;
        }
        public void setProperNames(List<String> properNames) {
            this.properNames = properNames;
        }
    }

    // using enum for value in the trie to save memory - otherwise there will be different instance for each form
    public static enum InflectedFormType {
        PLURAL_FORM(true, false, false),
        PLURAL_FORM_SPECIAL(true, true, false),
        PLURAL_FORM_VERB(true, false, true),
        REGULAR_FORM(false, false, false),
        REGULAR_FORM_SPECIAL(false, true, false),
        REGULAR_FORM_VERB(false, false, true),
        NOT_INFLECTABLE(false, false, false);

        private boolean plural;
        private boolean specialCaseNoun;
        private boolean verb;

        private InflectedFormType(boolean plural, boolean specialCaseNoun, boolean verb) {
            this.plural = plural;
            this.specialCaseNoun = specialCaseNoun;
            this.verb = verb;
        }

        public boolean isPlural() {
            return plural;
        }
        public boolean isSpecialCaseNoun() {
            return specialCaseNoun;
        }

        public boolean isVerb() {
            return verb;
        }
    }
}