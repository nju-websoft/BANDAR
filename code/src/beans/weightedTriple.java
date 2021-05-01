package beans;

public class weightedTriple implements Comparable<weightedTriple>{
    /**used in KSD snippet generation
     * @DATE: 20200315
     * */
    private int s, p, o;
    public double weight = 0; //the overall weight for sorting triples
    public double kwsW = 0, prpW = 0, clsW = 0, outW = 0, inW = 0;

    public void setSid(int arg){s = arg;}
    public int getSid(){return s;}

    public void setPid(int arg){p = arg;}
    public int getPid(){return p;}

    public void setOid(int arg){o = arg;}
    public int getOid(){return o;}

    public void setW() {//权重为四部分的组合
        weight = 2*kwsW + prpW + clsW + outW + inW;
    }

    public int compareTo(weightedTriple object) {
        if (this.weight > (object.weight))//descending order
            return -1;
        else if (this.weight == object.weight)
            return 0;
        else return 1;
    }
}
