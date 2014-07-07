package bg.bozho.ikratko.other;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.ardverk.collection.PatriciaTrie;
import org.ardverk.collection.StringKeyAnalyzer;
import org.ardverk.collection.Trie;

import com.google.common.collect.Sets;

import bg.bozho.ikratko.Checker;
import bg.bozho.ikratko.Checker.InflectedFormType;

public class Echo {
    private static final Set<String> commonPrefixes = Sets.newHashSet("раз", "пред", "през", "от", "не", "над", "под");
    public static void main(String[] args) throws Exception {
        Checker c = new Checker();
        c.initialize();

        Trie<String, InflectedFormType>reverse = new PatriciaTrie<String, InflectedFormType>(StringKeyAnalyzer.CHAR);
        for (Entry<String, Checker.InflectedFormType> entry: Checker.formsDictionary.entrySet()) {
            //using a StringBuilder so that no entry is placed in the jvm string pool
            String key = new StringBuilder(entry.getKey()).reverse().toString();
            reverse.put(key, entry.getValue());
        }
        FileOutputStream fos = new FileOutputStream("c:/var/echos.txt");
        OutputStreamWriter out = new OutputStreamWriter(fos, "utf-8");
        for (String form : c.formsDictionary.keySet()) {
            if (form.length() > 2) {
                String reversedForm = StringUtils.reverse(form);
                Set<String> echoesReversed = reverse.prefixMap(reversedForm).keySet();
                StringBuilder sb = new StringBuilder();
                String delim = "";
                for (String echoReversed : echoesReversed) {
                    String echo = StringUtils.reverse(echoReversed);
                    // exclude the same word and any word that is formed directly from it and another word or common prefix
                    String diff = echo.replace(form, "");
                    if (diff.length() == 1) {
                        diff = ""; // ignore 1-letter diffs
                    }
                    if (form.equals("античен")) {
                        diff = "";
                    }
                    if (!echo.equals(form)
                            && !c.formsDictionary.containsKey(diff)
                            && !commonPrefixes.contains(diff)) {
                        sb.append(delim + echo);
                        delim = ", ";
                    }
                }
                if (sb.length() > 0) {
                    sb.insert(0, form + ": ");
                    sb.append("\r\n");
                }
                out.write(sb.toString());
            }
        }
        out.close();
    }
}
