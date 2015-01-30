package bg.bozho.ikratko.other;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.collections4.trie.PatriciaTrie;

import bg.bozho.ikratko.Checker;
import static bg.bozho.ikratko.Checker.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;

public class NewsSitesVocabulary {

    private static Map<String, String> feeds = Maps.newLinkedHashMap();
    static {
        feeds.put("24 часа", "http://www.24chasa.bg/Rss.asp");
        feeds.put("Блиц", "http://www.blitz.bg/rss.php?news");
        feeds.put("БиНюз", "http://www.bnews.bg/rss.php");
        feeds.put("Вести", "http://www.vesti.bg/rss");
        feeds.put("Дарик нюз", "http://dariknews.bg/rss.php");
        feeds.put("Дневник", "http://www.dnevnik.bg/rss/");
        feeds.put("Днес.бг", "http://rss.dnes.bg/c/33162/f/539026/index.rss?today");
        feeds.put("Капитал", "http://www.capital.bg/rss/");
        feeds.put("Монитор", "http://monitor.bg/rss?id=1");
        feeds.put("ПИК", "http://pik.bg/rss/index/2");
        feeds.put("Сега", "http://www.segabg.com/rss20.xml");
        feeds.put("Стандарт", "http://www.standartnews.com/rss.php?p=1");
        feeds.put("Труд", "http://www.trud.bg/rss.asp");
        feeds.put("Хроникъл", "http://chronicle.bg/feed/");
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        new Checker().initialize();
        PatriciaTrie<String> forms = getFormsDictionaryReferencingBaseForm();
        System.out.println(StringUtils.rightPad("Издание", 30) + "думи |корени| к/д  | статии");
        Set<String> visited = Sets.newHashSet();

        Map<String, List<String>> accumulatedWords = Maps.newLinkedHashMap();
        Map<String, Integer> entriesCounts = Maps.newHashMap();

        for (int i = 0; i < 10; i++) {
            for (String site : feeds.keySet()) {
                if (!accumulatedWords.containsKey(site)) {
                    accumulatedWords.put(site, Lists.<String>newArrayList());
                    entriesCounts.put(site, 0);
                }

                try {
                    String url = feeds.get(site);
                    StringBuilder text = new StringBuilder();
                    SyndFeedInput input = new SyndFeedInput();
                    int entriesCount = 0;
                    try (InputStream in = new URL(url).openStream()) {
                        SyndFeed feed = input.build(new InputStreamReader(in));
                        List<SyndEntry> entries = feed.getEntries();
                        for (SyndEntry entry : entries) {
                            if (visited.contains(entry.getUri())) {
                                continue;
                            }
                            visited.add(entry.getUri());
                            entriesCount ++;
                            // adding the whole text to the accumulated text, stripping the last unfinished word
                            text.append(" " + entry.getDescription().getValue().replaceAll(" [\\\\pL\\\\pM\\\\p{Nd}\\\\p{Nl}\\\\p{Pc}[\\\\p{InEnclosedAlphanumerics}&&\\\\p{So}]]...", ""));
                        }
                    }
                    List<String> words = Arrays.asList(text.toString().split("[^\\pL\\pM\\p{Nd}\\p{Nl}\\p{Pc}[\\p{InEnclosedAlphanumerics}&&\\p{So}]]+"));
                    accumulatedWords.get(site).addAll(words);
                    entriesCounts.put(site, entriesCounts.get(site) + entriesCount);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            Thread.sleep(30 * 60 * 1000);
        }

        for (String site : accumulatedWords.keySet()) {
            List<String> words = accumulatedWords.get(site);
            Set<String> roots = Sets.newHashSet();
            double totalWords = 0;
            for (String word : words) {
                if (StringUtils.isNotBlank(word) && Character.isLowerCase(word.charAt(0)) && Checker.formsDictionary.containsKey(word)) {
                    totalWords ++;
                    roots.add(forms.get(word));
                }
            }
            System.out.println(StringUtils.rightPad(site, 30)
                    + StringUtils.rightPad(String.valueOf((int) totalWords), 4) + " | "
                    + StringUtils.rightPad(String.valueOf(roots.size()), 4) + " | "
                    + String.format("%.2f", roots.size() / totalWords) + " | "
                    + entriesCounts.get(site));
        }
    }


    public static PatriciaTrie<String> getFormsDictionaryReferencingBaseForm() {
        PatriciaTrie<String> trie = new PatriciaTrie<>();
        load();
        for (Map.Entry<String, Set<String>> word : dictionary.entrySet()) {
            String baseForm = word.getKey();
            if (word.getValue().isEmpty()) {
                trie.put(baseForm, baseForm);
                continue;
            }
            for (String inflectionClass : word.getValue()) {
                Multimap<String, String> inflections = inflectionClasses.get(inflectionClass);
                if (inflections == null) {
                    trie.put(baseForm, baseForm);
                    continue;
                }

                for (String ending : inflections.keySet()) {
                    int endingIdx = baseForm.lastIndexOf(ending);
                    if (!baseForm.endsWith(ending) || endingIdx == -1) {
                        continue;
                    }
                    trie.put(baseForm, baseForm);

                    for (String suffix : inflections.get(ending)) {
                        String inflectedWord = baseForm.substring(0, endingIdx) + suffix;
                        trie.put(inflectedWord, baseForm);
                    }
                }
            }
        }

        // override the forms of the verb "to be"
        for (String sgForm : toBeFormsSg) {
            trie.put(sgForm, "съм");
        }
        for (String plForm : toBeFormsPl) {
            trie.put(plForm, "съм");
        }

        dictionary = null; // eligible for GC. TODO can merge these two load methods, but it's easier not to, for now
        return trie;
    }
}
