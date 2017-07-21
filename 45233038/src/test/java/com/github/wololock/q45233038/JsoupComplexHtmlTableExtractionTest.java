package com.github.wololock.q45233038;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class JsoupComplexHtmlTableExtractionTest {

    @Test
    public void testExtractingComplexDataFromHtmlTableWithColspanAndRowspan() throws IOException {
        //given:
        final InputStream html = getClass().getClassLoader().getResourceAsStream("table.html");

        //when:
        final Document document = Jsoup.parse(html, "UTF-8", "/");

        final List<List<String>> result = document.select("table tr")
                .stream()
                .map(tr -> tr.select("td"))
                .map(rows -> rows.stream()
                        .map(td -> Collections.nCopies(td.hasAttr("colspan") ? Integer.valueOf(td.attr("colspan")) : 1, td))
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList())
                )
                .map(ArrayList::new)
                .reduce(new ArrayList<ArrayList<Element>>(), (acc, row) -> {
                    if (acc.isEmpty()) {
                        acc.add(row);
                        return acc;
                    }

                    final List<Element> last = acc.get(acc.size() - 1);
                    if (last.stream().noneMatch(td -> td.hasAttr("rowspan"))) {
                        acc.add(row);
                        return acc;
                    }

                    final AtomicInteger index = new AtomicInteger(0);
                    last.stream()
                            .map(td -> Arrays.asList(index.getAndIncrement(), Integer.valueOf(td.hasAttr("rowspan") ? td.attr("rowspan") : "0"), td))
                            .filter(it -> ((int) it.get(1)) > 1)
                            .forEach(it -> {
                                final int idx = (int) it.get(0);
                                final int rowspan = (int) it.get(1);
                                final Element td = (Element) it.get(2);

                                row.add(idx, rowspan - 1 == 0 ? (Element) td.removeAttr("rowspan") : td.attr("rowspan", String.valueOf(rowspan - 1)));
                            });

                    acc.add(row);
                    return acc;
                }, (a, b) -> a)
                .stream()
                .map(tr -> tr.stream()
                        .map(Element::text)
                        .collect(Collectors.toList())
                )
                .collect(Collectors.toList());

        //then:
        assertThat(result).hasSize(7);
        //and:
        assertThat(result.get(0)).hasSameElementsAs(Arrays.asList("H1", "H2", "H2"));
        //and:
        assertThat(result.get(1)).hasSameElementsAs(Arrays.asList("", "SubH2_1", "SubH2_2"));
        //and:
        assertThat(result.get(2)).hasSameElementsAs(Arrays.asList("A1", "B1", "C1"));
        //and:
        assertThat(result.get(3)).hasSameElementsAs(Arrays.asList("A1", "B2", "C3"));
        //and:
        assertThat(result.get(4)).hasSameElementsAs(Arrays.asList("C4", "C5", "C6"));
        //and:
        assertThat(result.get(5)).hasSameElementsAs(Arrays.asList("D7", "D9", "D9"));
        //and:
        assertThat(result.get(6)).hasSameElementsAs(Arrays.asList("Notes", "Notes", "Notes"));

        result.forEach(System.out::println);
    }
}
