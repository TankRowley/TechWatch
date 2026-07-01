package com.example.techwatch.keyword;
import com.example.techwatch.article.Article; import org.junit.jupiter.api.Test;
import java.time.Instant; import java.util.List; import static org.junit.jupiter.api.Assertions.*;
class KeywordExtractorTest {
 @Test void aliasesPluralHyphenAndJapaneseAdjacency(){
  var k8s=new Keyword("Kubernetes","Cloud","Watch",4,List.of("K8s")); var agent=new Keyword("AI Agent","AI","Watch",4);
  var article=Article.fetched(1L,"Blog","K8s実践とAI-Agents","https://e/a",Instant.now(),"");
  assertEquals(2,new KeywordExtractor().extract(article,List.of(k8s,agent)).size());
 }
 @Test void bodyAliasIsSeparate(){
  var postgres=new Keyword("PostgreSQL","Data","Core",4,List.of("Postgres"));
  var article=Article.fetched(1L,"Blog","Database notes","https://e/b",Instant.now(),"General");
  assertEquals(java.util.Set.of("body"),new KeywordExtractor().extract(article,"Use Postgres.",List.of(postgres)).getFirst().detectedIn());
 }
}
