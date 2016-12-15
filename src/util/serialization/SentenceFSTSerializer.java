package util.serialization;

import illinoisParser.LexicalToken;
import illinoisParser.Sentence;

import java.io.IOException;

import org.nustaq.serialization.FSTBasicObjectSerializer;
import org.nustaq.serialization.FSTClazzInfo;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;
import org.nustaq.serialization.util.FSTUtil;

public class SentenceFSTSerializer extends FSTBasicObjectSerializer {
    @Override
    public void writeObject(FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException {
    	Sentence sen = (Sentence) toWrite;
        out.writeInt(sen.getID());
        out.writeObjectInternal(sen.getTokens(), null, (Class[]) null);
        out.writeObjectInternal(sen.getCCGbankParse(), null, (Class[]) null);
    }
    

    @Override
    public void readObject(FSTObjectInput in, Object toRead, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy)
    {
    	try {
    	Sentence sen = (Sentence) toRead;
    	sen.setID(in.readInt());
    	sen.setSentence((LexicalToken[]) in.readObjectInternal());
    	sen.addCCGbankParse((String) in.readObjectInternal());
    	}
    	catch(Exception e) {
    		e.printStackTrace();
    	}
    }

    @SuppressWarnings({ "unused", "rawtypes" })
	@Override
    public Object instantiate(Class objectClass, FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPositioin) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        try {
        	//Sentence sen= new Sentence();
    		int id = in.readInt();
    		LexicalToken[] sentence = (LexicalToken[]) in.readObjectInternal();
    		String autoString = (String) in.readObjectInternal();
    		Sentence sen = new Sentence(sentence, id, autoString);
            //in.registerObject(sen, streamPositioin, serializationInfo, referencee);
    		return sen;
        } catch (Throwable th) {
            FSTUtil.<RuntimeException>rethrow(th);
        }
        return null;
    }

}
