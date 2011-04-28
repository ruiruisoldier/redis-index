/*
 * Copyright (c) 2002-2009 "Neo Technology,"
 *     Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 * 
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.index.redis;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.neo4j.index.base.IndexDataSource;
import org.neo4j.index.base.keyvalue.KeyValueCommand;
import org.neo4j.kernel.impl.transaction.xaframework.XaCommand;
import org.neo4j.kernel.impl.transaction.xaframework.XaDataSource;
import org.neo4j.kernel.impl.transaction.xaframework.XaLogicalLog;
import org.neo4j.kernel.impl.transaction.xaframework.XaTransaction;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * An {@link XaDataSource} optimized for the {@link LuceneIndexProvider}.
 * This class is public because the XA framework requires it.
 */
public class RedisDataSource extends IndexDataSource
{
    static final char KEY_DELIMITER = ':';
    static final String NAME = "redis";
    static final byte[] BRANCH_ID = "redis".getBytes();
    
    private final JedisPool db;
    
    /**
     * Constructs this data source.
     * 
     * @param params XA parameters.
     * @throws InstantiationException if the data source couldn't be
     * instantiated
     */
    public RedisDataSource( Map<Object,Object> params ) 
        throws InstantiationException
    {
        super( params );
        
        // TODO read config somehow... not just "localhost"
        GenericObjectPool.Config jedisPoolConfig = new GenericObjectPool.Config();
        db = new JedisPool( jedisPoolConfig, "localhost" );
    }

    @Override
    protected void actualClose()
    {
        // TODO shutdown redis
        db.destroy();
    }
    
    protected XaTransaction createTransaction( int identifier,
        XaLogicalLog logicalLog )
    {
        return new RedisTransaction( identifier, logicalLog, this );
    }
    
    @Override
    protected XaCommand readCommand( ReadableByteChannel channel, ByteBuffer buffer )
            throws IOException
    {
        return KeyValueCommand.readCommand( channel, buffer, this );
    }

    @Override
    protected void flushAll()
    {
        // TODO
    }

    public Jedis acquireResource()
    {
        return db.getResource();
    }
    
    public void releaseResource( Jedis resource )
    {
        db.returnResource( resource );
    }

    public String formRedisKey( String indexName, String key, String value )
    {
        return new StringBuilder( indexName ).append( KEY_DELIMITER ).append( key ).append( KEY_DELIMITER )
                .append( value ).toString();
    }
}