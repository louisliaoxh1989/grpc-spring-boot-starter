/*
 * Copyright (c) 2016-2018 Michael Zhang <yidongnan@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.devh.boot.grpc.client.nameresolver;

import static java.util.Objects.requireNonNull;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import net.devh.boot.grpc.client.config.GrpcChannelProperties;

/**
 * A name resolver factory that will create a {@link NameResolver} with static addresses. This factory uses the
 * {@link #STATIC_SCHEME "static" scheme}.
 *
 * @author Daniel Theuke (daniel.theuke@heuboe.de)
 */
public class StaticNameResolverFactory extends NameResolverProvider {

    /**
     * The constant containing the scheme that will be used by this factor.y
     */
    public static final String STATIC_SCHEME = "static";

    private static final Pattern PATTERN_COMMA = Pattern.compile(",");

    @Nullable
    @Override
    public NameResolver newNameResolver(final URI targetUri, final Attributes params) {
        if (STATIC_SCHEME.equals(targetUri.getScheme())) {
            return of(targetUri.getAuthority(), params);
        }
        return null;
    }

    /**
     * Creates a new {@link NameResolver} for the given authority and attributes.
     *
     * @param targetAuthority The authority to connect to.
     * @param params Optional parameters that customize the resolve process.
     * @return The newly created name resolver for the given target.
     */
    private NameResolver of(final String targetAuthority, final Attributes params) {
        // Determine target ips
        final String[] hosts = PATTERN_COMMA.split(targetAuthority);
        final List<SocketAddress> targets = new ArrayList<>(hosts.length);
        for (final String host : hosts) {
            final URI uri = URI.create("//" + host);
            int port = uri.getPort();
            if (port == -1) {
                final Integer defaultPort = params.get(NameResolver.Factory.PARAMS_DEFAULT_PORT);
                if (defaultPort == null) {
                    port = GrpcChannelProperties.DEFAULT_PORT;
                } else {
                    port = defaultPort;
                }
            }
            targets.add(new InetSocketAddress(uri.getHost(), port));
        }
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("Must have at least one target, but was: " + targetAuthority);
        }
        return new StaticNameResolver(targetAuthority, new EquivalentAddressGroup(targets));
    }

    @Override
    public String getDefaultScheme() {
        return STATIC_SCHEME;
    }

    @Override
    protected boolean isAvailable() {
        return true;
    }

    @Override
    protected int priority() {
        return 5; // Default priority
    }

    /**
     * A {@link NameResolver} that will always respond with a static set of target addresses.
     */
    public static class StaticNameResolver extends NameResolver {

        private final String authority;
        private final List<EquivalentAddressGroup> targets;

        public StaticNameResolver(final String authority, final EquivalentAddressGroup target) {
            this(authority, ImmutableList.of(requireNonNull(target, "target")));
        }

        public StaticNameResolver(final String authority, final Collection<EquivalentAddressGroup> targets) {
            this.authority = requireNonNull(authority, "authority");
            if (requireNonNull(targets, "targets").isEmpty()) {
                throw new IllegalArgumentException("Must have at least one target");
            }
            this.targets = ImmutableList.copyOf(targets);
        }

        @Override
        public String getServiceAuthority() {
            return this.authority;
        }

        @Override
        public void start(final Listener listener) {
            listener.onAddresses(this.targets, Attributes.EMPTY);
        }

        @Override
        public void refresh() {
            // Does nothing
        }

        @Override
        public void shutdown() {
            // Does nothing
        }

    }

}
