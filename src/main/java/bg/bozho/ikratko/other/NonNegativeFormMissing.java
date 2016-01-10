package bg.bozho.ikratko.other;

import bg.bozho.ikratko.Checker;

/**
 * Намиране на всички думи, които започват с "не", но форма без "не" не съществува
 * @author bozhanov
 *
 */
public class NonNegativeFormMissing {

    public static void main(String[] args) {
        Checker c = new Checker();
        c.initialize(false);
        
        for (String word : Checker.dictionary.prefixMap("не").keySet()) {
            String n = word.replaceFirst("не", "");
            if (!Checker.formsDictionary.containsKey(n)) {
                System.out.println(n);
            }
        }
    }
}
