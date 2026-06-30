package com.example.techwatch.fetch;

import com.example.techwatch.source.Source;

public interface FeedFetcher {
    FeedFetchResult fetch(Source source);
}
