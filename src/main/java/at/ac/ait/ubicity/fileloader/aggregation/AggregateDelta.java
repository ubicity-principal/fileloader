

package at.ac.ait.ubicity.fileloader.aggregation;

import com.lmax.disruptor.EventFactory;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author jan van oort
 */
public final class AggregateDelta {

    
    static EventFactory EVENT_FACTORY = () -> new AggregateDelta();
    
    
    public WeakReference< AtomicLong > delta;
    
    
    
    public AggregateDelta( long _delta )  {
        delta = new WeakReference( new AtomicLong( _delta ) );
    }

    
    private AggregateDelta() {
        
    }
}
