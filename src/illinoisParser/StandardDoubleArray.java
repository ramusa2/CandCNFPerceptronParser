package illinoisParser;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

public class StandardDoubleArray implements Externalizable {
    
        private double[] da = new double[] {};
        private int size = 0;
        private final static double INCREMENT = 1.5;
        private int length = 0;
        private double sum = 0;
        private boolean valid = true;
        
        public StandardDoubleArray() {}
        public StandardDoubleArray(int s){
            this.da = new double[s];
            Arrays.fill(da,0.0);
            this.size = s;
            this.length = s;
        }

        public StandardDoubleArray(StandardDoubleArray copy){
            this.da = Arrays.copyOf(copy.da, copy.da.length);
            this.size = copy.size;
            this.length = copy.length;
        }
        
        final double[] vals(){
            return da;
        }
        public final void set(int i, double v){
            if ( i >= length )
            {    
                this.da = Arrays.copyOf(da, (int) ((i * INCREMENT) + 1));
                Arrays.fill(da, length, da.length,0.0);
                this.length = da.length;
            }
            this.da[i] = v;
            this.valid = false;
        }
        
        final void add(double v){
            set(size,v);
            size++;
        }
        final synchronized DoubleArray collapse() throws Exception{
            double t = sum();
            clear();
            DoubleArray newA = new DoubleArray();
            newA.add(t);
            return newA;
        }
        public final double get(int i){
            return da[i];
        }
        final double sum() throws Exception{
            if (!valid) {
                sum = 0;
                for(double d : da)
                    sum += d;
                valid = true;
            }
            return sum;
        }
        final double prod() throws Exception{
            Arrays.sort(da);
            double product = 1.0;
            for(int i = da.length - 1; i >= 0; i--){
                product = product * da[i];
            }
            return product;
        }
        final void clear(){
            zero();
            size = 0;
        }
        private final void zero() {
            Arrays.fill(da, 0.0);
            sum = 0.0;
            valid = true;
        }
        @Override
        public void readExternal(ObjectInput in) throws IOException,
                ClassNotFoundException {
            da = (double[]) in.readObject();
            length = da.length;
            size = in.readInt();
            sum = in.readDouble();
            valid = in.readBoolean();
        }
        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(da);
            out.writeInt(size);
            out.writeDouble(sum);
            out.writeBoolean(valid);
        }
        public void addAll(double[] arr){
            for (double d : arr){
                add(d);
            }
        }
        public String toString(){
            return Arrays.toString(Arrays.copyOf(da,size));
        }
}
