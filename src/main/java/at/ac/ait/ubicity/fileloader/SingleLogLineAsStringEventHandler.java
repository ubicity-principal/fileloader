package at.ac.ait.ubicity.fileloader;

/**
    Copyright (C) 2013  AIT / Austrian Institute of Technology
    http://www.ait.ac.at

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see http://www.gnu.org/licenses/agpl-3.0.html
 */


import static at.ac.ait.ubicity.fileloader.cassandra.AstyanaxInitializer.log;
import at.ac.ait.ubicity.fileloader.cassandra.LogLineColumn;

import com.lmax.disruptor.EventHandler;

import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.exceptions.OperationTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Date;

import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 *
 * @author Jan van Oort
 */
final class SingleLogLineAsStringEventHandler implements EventHandler<SingleLogLineAsString> {

    
    
    static int batchSize; 
    
    
    final static Logger logger = Logger.getLogger( "EventHandler" );
    
    
    static Keyspace keySpace;
    
    static MutationBatch batch;
    
    static long _waitOnTimeOut = 500;
    
    static final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" );

    /**
     * No-arg constructor, necessary by contract with LMAX Disruptor 
     */
    public SingleLogLineAsStringEventHandler( ) {
    }
    
    
    /**
     * 
     * We will, foreseeably, never need LogLineTokenizer  for some
     * other purpose than for having its implementation handy here; hence, we might
     * as well use a lambda expression, which saves us some maintenance pain. 
     */
    
    
    
    
    
    @Override
    public final void onEvent(final SingleLogLineAsString event, final long sequence, final boolean endOfBatch) throws Exception {
    
        /**
         * Tokenize the payload of our event, and use a database column schema 
         * to attribute the right token to the right column.
         * Take the LMAX ringbuffer sequence number as line_id, always works; 
         * also has the advantage that line_ids in the database *always* mirror the exact order of insertion; 
         * exact order of insertion = exact order of line loading into the ringbuffer = exact order of reading from the log file
         * 
         */
        
        
        String[] __tokens = new String[ 14 ];
        int _counter = 0;
        StringTokenizer _stokenizer = new StringTokenizer( event.value, " " );
        while( _stokenizer.hasMoreTokens( ) )    {
            __tokens[ _counter ] = _stokenizer.nextToken();
            _counter++;
        }
        __tokens[ 12 ] = event.value;
        __tokens[ 13 ] = __tokens[ 0 ];
        __tokens[ 14 ] = Long.toString( dateFormat.parse( __tokens[ 0 ] ).getTime() );
        
        LogLineColumn _col = LogLineColumn.ID; 

        for ( int i = 0; i < 14; i++ )   {
           String __tok = __tokens[ i ];
           if (  ( _col = _col.next() ) != LogLineColumn.NONE )  {
                batch.withRow( log, Long.toString( sequence ) ).putColumn( _col.name, __tok );
           }
        }
        

        try {
            if( sequence % batchSize == 0 ) {
                batch.executeAsync().get();
                System.out.print( sequence + " " );
            }
        }
        catch( ExecutionException | OperationTimeoutException  opTimedOut )  {
            try {
                logger.warning( "backing off for " + _waitOnTimeOut + " millis before retrying" );
                logger.warning( "here comes the stack trace:\n" );
                opTimedOut.printStackTrace();
                Thread.sleep( _waitOnTimeOut );
                //exponential back-off
                _waitOnTimeOut = 2 * _waitOnTimeOut;
                //try again
                onEvent( event, sequence, endOfBatch );
            }
            catch( InterruptedException interrupt ) {
                Thread.interrupted();
            }
        }
        catch( ConnectionException e )  {
            logger.log( Level.SEVERE, e.toString() );
        }
    }
}
