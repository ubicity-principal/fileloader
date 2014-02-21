
package at.ac.ait.ubicity.fileloader.util;

import at.ac.ait.ubicity.fileloader.cassandra.AstyanaxInitializer;
import com.google.common.collect.ImmutableMap;
import com.netflix.astyanax.ExceptionCallback;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.exceptions.BadRequestException;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.serializers.LongSerializer;
import com.netflix.astyanax.serializers.StringSerializer;
import com.netflix.astyanax.util.RangeBuilder;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.cassandra.db.ColumnSerializer;

/**
 *
 * @author jan
 */
public final class CheckDB {
    
    
    
    public final static void main( String... args ) throws Exception {
        
        Logger logger = Logger.getLogger( CheckDB.class.getName() );
        logger.setLevel(Level.ALL);

        Rows< Long, String > rows = null;
        Keyspace keySpace = AstyanaxInitializer.doInit( "Test Cluster", "localhost", "CF_LOGLINES" );
        ColumnFamily< Long, String > cf = null;
        
        
        try {
            
               cf = AstyanaxInitializer.CF_LOGLINES;
               
            rows = keySpace.prepareQuery( cf ).getAllRows().setBlockSize( 10 ).withColumnRange( new RangeBuilder().setMaxSize( 10).build() )
                    .setExceptionCallback( new ExceptionCallback()  {
                        @Override
                        public boolean onException( ConnectionException e ) {
                            try {
                                Thread.sleep( 1000 );
                            }
                            catch( Exception eee )    {
                                eee.printStackTrace();
                            }
                            return true;
                    }})
                    .execute().getResult();
        }
        catch( BadRequestException bre )    {
            logger.log( Level.INFO,  "column space " + cf.getName() + " exists, everything OK, proceeding... " ) ;
        }    
        catch( ConnectionException noCassandra )    {
            logger.log( Level.SEVERE, noCassandra.toString() );
            noCassandra.printStackTrace();
        }
        System.out.println( "!!!! rows == null ? " + ( rows == null ) );
        Iterator onRows = rows.iterator();
        int i = 0;
        while( onRows.hasNext() )   {
            System.out.println( "\n" );
            Row< Long, String > _row = ( Row ) onRows.next();
            //System.out.println( "[ROW] :: " + _row.getKey() + " :: " + _row.getColumns().size() );
            for( Column<String> c : _row.getColumns() ) {
                System.out.print( " " + c.getName() );
                System.out.print( " " + c.getStringValue() );
            }
            i++;
            //if( i > 5 ) break;
        }
    }
}
