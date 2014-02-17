
package at.ac.ait.ubicity.fileloader.aggregation;

import static at.ac.ait.ubicity.fileloader.FileLoader.TWO;
import at.ac.ait.ubicity.fileloader.cassandra.AstyanaxInitializer;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jan
 */
public final class AggregationJob {
    
    
    
    private Logger logger = Logger.getLogger( this.getClass().getName() );
    
    
    public AggregationJob() {
        
    }
    
    
    
    public final void run( final String _keySpace, final String _host, final int _batchSize ) throws Exception {
        Keyspace keySpace = AstyanaxInitializer.doInit( "Test Cluster", _host, _keySpace );
        final MutationBatch batch = keySpace.prepareMutationBatch();
        
        logger.setLevel( Level.ALL );
        logger.info( "got keyspace " + keySpace.getKeyspaceName() + " from Astyanax initializer" );
        
        final ExecutorService exec = Executors.newFixedThreadPool( Runtime.getRuntime().availableProcessors() * 2 );        
        final Disruptor< AggregateDelta > disruptor = new Disruptor( AggregateDelta.EVENT_FACTORY, ( int ) Math.pow( TWO, 10 ), exec );
        
        final EventHandler< AggregateDelta > downloadVolumehandler = new Aggregator( new DownloadVolume() );
        final EventHandler< AggregateDelta > numberOfURLsCrawledHandler = new Aggregator( new NumberOfURLsCrawled() );
        disruptor.handleEventsWith( downloadVolumehandler, numberOfURLsCrawledHandler );
        
        
        final RingBuffer< AggregateDelta > rb = disruptor.start();
        
        int _lineCount = 0;
        long _start, _lapse;
        _start = System.nanoTime();
        
    
    }
    
    
    
    
    public final static void main( String... args ) {
        
    }
}
