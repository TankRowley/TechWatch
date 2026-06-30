package com.example.techwatch.market;

import com.example.techwatch.keyword.Keyword;

import java.nio.file.Path;
import java.util.List;

public interface JobMarketSource {
    List<JobMarketSnapshot> load(Path source, List<Keyword> keywords) throws Exception;
}
