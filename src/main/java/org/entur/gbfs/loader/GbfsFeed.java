package org.entur.gbfs.loader;

import java.net.URI;

public record GbfsFeed<S, T>(S name, Class<T> implementingClass, URI uri) {}
