package com.protegra_ati.agentservices.core.util.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.protegra_ati.agentservices.core.util.ReportingImpl4Java;
import org.apache.commons.pool.impl.StackObjectPool;

import java.io.*;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: mtodd
 * Date: 03/01/13
 * Time: 2:59 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class KryoSerializerBase extends AbstractToStringSerializer {


    protected static final String HEADER_4_STRING_SERIALIZATION = "KryoSerializer_";
    protected abstract StackObjectPool getPoolImpl();

    public static KryoSerializerBase getInstance()
    {
        return null;
    }
    /**
     * Serializes object into stream using 'Kryo' API
     *
     * @param toBeSerialized object
     * @param intoStream     target
     */

    public void serialize( Object toBeSerialized, OutputStream intoStream )
    {
        Kryo serializer = null;
        Output buffer = new Output( 5000, -1 );
        buffer.setOutputStream(intoStream);
        //final Output buffer = new Output( intoStream, 5000 );
        try {
            try {
                serializer = (Kryo) getPoolImpl().borrowObject();

            } catch ( Exception e ) {
                //   logger.report( "can't borrow serializer from a pool" + e.getMessage(), Severity.Error() );
            }
            serializer.writeClassAndObject( buffer, toBeSerialized );


        } catch ( Exception e ) {
            e.printStackTrace();
            // logger.report( "object " + toBeSerialized + "  can't be serialized:" + e.getMessage(), Severity.Error() );
        } catch ( Error er ) {
            er.printStackTrace();
            //  logger.report( "object " + toBeSerialized + "  can't be serialized:" + er.getMessage(), Severity.Error() );
        } finally {

            if ( buffer != null ) {
                try {
                    buffer.flush();
                    buffer.clear();
                    buffer.close();
                } catch ( Exception e ) {
                    //   logger.report( "buffer can't be closed:" + e.getMessage(), Severity.Warning() );
                }
            }

            if ( serializer != null ) {
                try {
                    getPoolImpl().returnObject( serializer );
                } catch ( Exception e ) {
                    //  logger.report( "can't return serializer back to the pool:" + e.getMessage(), Severity.Warning() );
                }
            }
        }
    }

    /**
     * Serializes object into String using 'Kryo' API and after it base 64 encoder
     *
     * @param objToBeSerialized to be serialized
     * @return base64 encoded object byte stream
     */
    protected String serializeRaw( Object objToBeSerialized )
    {
        String uid5CharLong = UUID.randomUUID().toString().substring( 0, 5 );
//        System.err.println( "#####-serialize KRYO--ID:" + uid5CharLong + "--: " + objToBeSerialized );
        // logger.report( "#####-serialize KRYO--ID:" + uid5CharLong + "--: " + objToBeSerialized, Severity.Warning() );
        BufferedOutputStream oos = null;
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //try {
        oos = new BufferedOutputStream( baos );
        // } catch ( IOException e ) {/*can never happen here*/}
        serialize( objToBeSerialized, oos );  // closes the stream
        final byte[] bArray = baos.toByteArray();
        serializationErrorCheck( bArray, objToBeSerialized );
        final String encodedMsg = new String( biz.source_code.base64Coder.Base64Coder.encode( bArray ) );
        sizeWarning( encodedMsg, ( objToBeSerialized ) );
        return uid5CharLong + encodedMsg;
    }

    public String getHeader()
    {
        // returns always the type
        return HEADER_4_STRING_SERIALIZATION;
    }

    public  <T> T deserialize( InputStream fromInputStream )
    {
        return deserialize(fromInputStream, 5000);
    }
    /**
     * deserializes object of type T from given InputStream
     *
     * @param fromInputStream source
     * @param <T>             desired type
     * @return object of type T or null
     */
    protected  <T> T deserialize( InputStream fromInputStream, int bufferSize )
    {
        Kryo serializer = null;
        Object data = null;
        try {
            try {
                serializer = (Kryo) getPoolImpl().borrowObject();
            } catch ( Exception e ) {
                //     logger.report( "ERROR: can't borrow serializer from a pool" + e.getMessage(), Severity.Error() );
            }
            final Input buffer = new Input( fromInputStream, bufferSize );
            data = serializer.readClassAndObject( buffer );
            buffer.rewind();
            buffer.close();
            return (T) data;
        } catch ( Exception e ) {
            e.printStackTrace();
            //    logger.report( "ERROR: object " + data + " can't be deserialized:" + e.getMessage(), Severity.Error() );
        } catch ( Error er ) {
            er.printStackTrace();
            //   logger.report( "ERROR: object " + data + " can't be deserialized:" + er.getMessage(), Severity.Error() );
        } finally {
            if ( fromInputStream != null ) {
                try {
                    fromInputStream.close();
                } catch ( IOException e ) {
                    //  logger.report( "buffer can't be closed:" + e.getMessage(), Severity.Warning() );
                }
            }

            if ( serializer != null ) {
                try {
                    getPoolImpl().returnObject( serializer );
                } catch ( Exception e ) {
                    //   logger.report( "can't return serializer back to the pool:" + e.getMessage(), Severity.Warning() );
                }
            }
        }
        return (T) null;
    } // not reachable


    protected <T> T deserializeRaw( String source )
    {
        try {
            String uid = source.substring( 0, 5 );
//            System.err.println( "#####-deserialize KRYO-- UID:" + uid );
            //   logger.report( "#####-deserialize KRYO-- UID:" + uid, Severity.Warning() );
            // STRESS TODO uid has to be removed for production, it is just for debugging purposes
            final byte[] byteArrayMsg = biz.source_code.base64Coder.Base64Coder.decode( source.substring( 5, source.length() ) );
            final InputStream ois = new BufferedInputStream( new ByteArrayInputStream( byteArrayMsg ) );
            final T obj = deserialize( ois, byteArrayMsg.length ); // closes the stream
//            System.err.println( "#####-deserialize KRYO--ID:" + uid + " ----: " + obj );
            // logger.report( "#####-deserialize KRYO--ID:" + uid + " ----: " + obj, Severity.Warning() );
            return obj;
        } catch ( Exception e ) {
            //   logger.report( "ERROR: object can't be deserialized due to base64 decoding problem:" + e.getMessage(), Severity.Error() );
        }
        return (T) null;
    }

    private final void sizeWarning( String encodedMsg, Object obj )
    {
        long bytesInAKilobyte = 1024;
        long maxBytes = 100 * bytesInAKilobyte;
        //each char roughly 1 byte
        if ( encodedMsg.length() > maxBytes ) {
            //   logger.report( "serialized message is more than " + maxBytes / bytesInAKilobyte + " KB for obj " + obj.toString(), Severity.Warning() );
        }
    }

}