package branch_and_bound_vrptw;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Scanner;

//定义参数
class Data{
	int vertex_num;			//所有点集合n（包括配送中心和客户点，首尾（0和n）为配送中心）
	double E;	      		//配送中心时间窗开始时间
	double	L;	     		//配送中心时间窗结束时间
	int veh_num;    		//车辆数
	double cap;     		//车辆载荷
	double[][] vertexs;		//所有点的坐标x,y
	double[] demands;			//需求量
	int[] vehicles;			//车辆编号
	double[] a;				//时间窗开始时间【a[i],b[i]】
	double[] b;				//时间窗结束时间【a[i],b[i]】
	double[] s;				//客户点的服务时间
	int[][] arcs;			//arcs[i][j]表示i到j点的弧
	double[][] dist;		//距离矩阵，满足三角关系,暂用距离表示花费 C[i][j]=dist[i][j]
	double gap= 1e-6;
	double big_num = 100000;
	//截断小数3.26434-->3.2
	public double double_truncate(double v){
		int iv = (int) v;
		if(iv+1 - v <= gap)
			return iv+1;
		double dv = (v - iv) * 10;
		int idv = (int) dv;
		double rv = iv + idv / 10.0;
		return rv;
	}	
	public Data() {
		super();
	}
	//函数功能：从txt文件中读取数据并初始化参数
	public void Read_data(String path,Data data,int vertexnum) throws Exception{
		String line = null;
		String[] substr = null;
		Scanner cin = new Scanner(new BufferedReader(new FileReader(path)));  //读取文件
		for(int i =0; i < 4;i++){
			line = cin.nextLine();  //读取一行
		}
		line = cin.nextLine();
		line.trim(); //返回调用字符串对象的一个副本，删除起始和结尾的空格
		substr = line.split(("\\s+")); //以空格为标志将字符串拆分
		//初始化参数
		data.vertex_num = vertexnum;
		data.veh_num = Integer.parseInt(substr[1]); 
		data.cap = Integer.parseInt(substr[2]);
		data.vertexs =new double[data.vertex_num][2];				//所有点的坐标x,y
		data.demands = new double[data.vertex_num];					//需求量
		data.vehicles = new int[data.veh_num];					//车辆编号
		data.a = new double[data.vertex_num];						//时间窗开始时间
		data.b = new double[data.vertex_num];						//时间窗结束时间
		data.s = new double[data.vertex_num];						//服务时间
		data.arcs = new int[data.vertex_num][data.vertex_num];
		//距离矩阵,满足三角关系,用距离表示cost
		data.dist = new double[data.vertex_num][data.vertex_num];
		for(int i =0; i < 4;i++){
			line = cin.nextLine();
		}
		//读取vetexnum-1行数据
		for (int i = 0; i < data.vertex_num - 1; i++) {
			line = cin.nextLine();
			line.trim();
			substr = line.split("\\s+");
			data.vertexs[i][0] = Float.parseFloat(substr[2]);
			data.vertexs[i][1] = Float.parseFloat(substr[3]);
			data.demands[i] = Float.parseFloat(substr[4]);
			data.a[i] = Integer.parseInt(substr[5]);
			data.b[i] = Integer.parseInt(substr[6]);
			data.s[i] = Integer.parseInt(substr[7]);
		}
		cin.close();//关闭流
		//初始化配送中心参数
		data.vertexs[data.vertex_num-1] = data.vertexs[0];
		data.demands[data.vertex_num-1] = 0;
		data.a[data.vertex_num-1] = data.a[0];
		data.b[data.vertex_num-1] = data.b[0];
		data.E = data.a[0];
		data.L = data.b[0];
		data.s[data.vertex_num-1] = 0;		
		double min1 = 1e15;
		double min2 = 1e15;
		//距离矩阵初始化
//		for (int i = 0; i < data.vertex_num; i++) {
//			for (int j = 0; j < data.vertex_num; j++) {
//				if (i == j) {
//					data.dist[i][j] = 0;
//					continue;
//				}
//				data.dist[i][j] =
//					Math.sqrt((data.vertexs[i][0]-data.vertexs[j][0])
//							*(data.vertexs[i][0]-data.vertexs[j][0])+
//					(data.vertexs[i][1]-data.vertexs[j][1])
//					*(data.vertexs[i][1]-data.vertexs[j][1]));
//				data.dist[i][j]=data.double_truncate(data.dist[i][j]);
//			}
//		}
		data.dist[0][0] = 0;data.dist[0][1] = 0.76;data.dist[0][2] = 1.3;data.dist[0][3] = 2.4;data.dist[0][4] = 1.5;data.dist[0][5] = 2.8;data.dist[0][6] = 1.8;data.dist[0][7] = 2;
		data.dist[0][8] = 3.9;data.dist[0][8] = 2.8;data.dist[0][10] = 2.2;data.dist[0][11] = 2.5;data.dist[0][12] = 2.1;data.dist[0][13] = 1.9;data.dist[0][14] = 1.3;data.dist[0][15] = 1.3;
		data.dist[0][16] = 0.77;data.dist[0][17] = 1.2;data.dist[0][18] = 2.3;data.dist[0][19] = 2.3;data.dist[0][20] = 2.3;data.dist[0][21] = 1.6;data.dist[0][22] = 0.89;data.dist[0][23] = 1.2;
		data.dist[0][24] = 1;data.dist[0][25] = 0.71;data.dist[0][26] = 1.6;data.dist[0][27] = 3.2;data.dist[0][28] = 1.6;data.dist[0][29] = 1.8;
		data.dist[1][1] = 0;data.dist[1][2] = 2.2;data.dist[1][3] = 2.6;data.dist[1][4] = 2;data.dist[1][5] = 2.5;data.dist[1][6] = 1.2;data.dist[1][7] = 2.4;data.dist[1][8] = 3.3;data.dist[1][9] = 4.1;
		data.dist[1][10] = 2.5;data.dist[1][11] = 3;data.dist[1][12] = 1.9;data.dist[1][13] = 2.2;data.dist[1][14] = 0.75;data.dist[1][15] = 1.5;data.dist[1][16] = 0.56;data.dist[1][17] = 1.3;data.dist[1][18] = 2.5;
		data.dist[1][19] = 2.6;data.dist[1][20] = 2.6;data.dist[1][21] = 0.92;data.dist[1][22] = 0.49;data.dist[1][23] = 1;data.dist[1][24] = 0.77;data.dist[1][25] = 1.7;data.dist[1][26] = 0.96;data.dist[1][27] = 3;data.dist[1][28] = 2.5;data.dist[1][1] = 2.1;
		data.dist[2][2] = 0;data.dist[2][3] = 1.2;data.dist[2][4] = 1.8;data.dist[2][5] = 1.6;data.dist[2][6] = 3.1;data.dist[2][7] = 0.75;data.dist[2][8] = 2.9;data.dist[2][9] = 1.6;data.dist[2][10] = 1.1;data.dist[2][11] = 1.4;data.dist[2][12] = 3;data.dist[2][13] = 0.98;data.dist[2][14] = 1.9;data.dist[2][15] = 1.9;data.dist[2][16] = 2;data.dist[2][17] = 2.7;
		data.dist[2][18] = 1.6;data.dist[2][19] = 1.2;data.dist[2][20] = 1.6;data.dist[2][21] = 2.2;data.dist[2][22] = 2.2;data.dist[2][23] = 1.6;data.dist[2][24] = 2.3;data.dist[2][25] = 1.5;data.dist[2][26] = 2.3;data.dist[2][27] = 4.1;data.dist[2][28] = 0.38;data.dist[2][29] = 1.2;
		data.dist[3][3] = 0;data.dist[3][4] =2.6 ;data.dist[3][5] =2.1;data.dist[3][6] =3;data.dist[3][7] =0.47;
		data.dist[3][8]=2.8;data.dist[3][9]=1.2;data.dist[3][10]=0.65;data.dist[3][11]=0.71;data.dist[3][12]=2.7;
		data.dist[3][13] =0.82;data.dist[3][14]=2;data.dist[3][15]=2;data.dist[3][16]=2.7;data.dist[3][17]=2.7;
		data.dist[3][18] =2.4;data.dist[3][19]=0.4;data.dist[3][20]=2.4;data.dist[3][21]=2.3;data.dist[3][22]=2.9;
		data.dist[3][23] =1.6;data.dist[3][24]=3;data.dist[3][25]=1.7;data.dist[3][26]=2.4;data.dist[3][27]=3.9;
		data.dist[3][28] =1.1;data.dist[3][29]=1.1;
		data.dist[4][4] =0;data.dist[4][5]=1.9;data.dist[4][6]=2.9;data.dist[4][7]=2.2;data.dist[4][8]=1.9;
		data.dist[4][9] =3.4;data.dist[4][10]=2.5;data.dist[4][11]=2.8;data.dist[4][12]=3.1;data.dist[4][13]=2.5;
		data.dist[4][14] =2.3;data.dist[4][15]=2.4;data.dist[4][16]=1.9;data.dist[4][17]=3;data.dist[4][18]=0.94;
		data.dist[4][19] =2.6;data.dist[4][20]=0.97;data.dist[4][21]=2.6;data.dist[4][22]=2;data.dist[4][23]=2.1;
		data.dist[4][24] =2;data.dist[4][25]=1.4;data.dist[4][26]=2.8;data.dist[4][27]=4.2;data.dist[4][28]=1.8;data.dist[4][29]=2.5;
		data.dist[5][5] =0;data.dist[5][6]=4.4;data.dist[5][7]=2.4;data.dist[5][8]=0.9;data.dist[5][9]=1.7;data.dist[5][10]=2.8;
		data.dist[5][11] =3.1;data.dist[5][12]=4.6;data.dist[5][13]=2.7;data.dist[5][14]=3.7;data.dist[5][15]=3.7;data.dist[5][16]=3.4;
		data.dist[5][17] =4.5;data.dist[5][18]=1.4;data.dist[5][19]=2.9;data.dist[5][20]=0.93;data.dist[5][21]=4;data.dist[5][22]=3.9;
		data.dist[5][23] =3.4;data.dist[5][24]=2.5;data.dist[5][25]=2.8;data.dist[5][26]=4.1;data.dist[5][27]=5.2;data.dist[5][28]=2.1;data.dist[5][29]=2.9;
		data.dist[6][6] =0;data.dist[6][7]=2.9;data.dist[6][8]=4.4;data.dist[6][9]=5.1;data.dist[6][10]=3.1;data.dist[6][11]=3.5;
		data.dist[6][12] =2.4;data.dist[6][13]=2.7;data.dist[6][14]=1.3;data.dist[6][15]=2.1;data.dist[6][16]=1;data.dist[6][17]=1.5;
		data.dist[6][18] =3.5;data.dist[6][19]=3.2;data.dist[6][20]=3.6;data.dist[6][21]=1.1;data.dist[6][22]=1;data.dist[6][23]=1.6;
		data.dist[6][24] =1.8;data.dist[6][25]=2.3;data.dist[6][26]=1.1;data.dist[6][27]=3.6;data.dist[6][28]=3.5;data.dist[6][29]=2.7;
		data.dist[7][7] =0;data.dist[7][8]=2.9;data.dist[7][9]=1.5;data.dist[7][10]=0.49;data.dist[7][11]=0.55;data.dist[7][12]=2.6;
		data.dist[7][13] =0.66;data.dist[7][14]=1.7;data.dist[7][15]=1.6;data.dist[7][16]=2.4;data.dist[7][17]=2.4;data.dist[7][18]=2;
		data.dist[7][19] =0.4;data.dist[7][20]=2;data.dist[7][21]=2;data.dist[7][22]=1.9;data.dist[7][23]=1.3;data.dist[7][24]=2.6;
		data.dist[7][25] =1.5;data.dist[7][26]=2.1;data.dist[7][27]=3.7;data.dist[7][28]=0.79;data.dist[7][29]=0.91;
		data.dist[8][8] =0;data.dist[8][9]=2.5;data.dist[8][10]=3;data.dist[8][11]=3.3;data.dist[8][12]=4.8;data.dist[8][13]=2.9;
		data.dist[8][14] =3.9;data.dist[8][15]=4;data.dist[8][16]=3.5;data.dist[8][17]=5.1;data.dist[8][18]=1.5;data.dist[8][19]=3.1;
		data.dist[8][20] =1;data.dist[8][21]=4.7;data.dist[8][22]=3.6;data.dist[8][23]=4;data.dist[8][24]=3.8;data.dist[8][25]=3.4;
		data.dist[8][26] =4.7;data.dist[8][27]=6.1;data.dist[8][28]=2.7;data.dist[8][29]=3.1;
		data.dist[9][9] =0;data.dist[9][10]=1;data.dist[9][11]=1.3;data.dist[9][12]=2.9;data.dist[9][13]=0.95;
		data.dist[9][14] = 2;data.dist[9][15]=1.9;data.dist[9][16]=2.1;data.dist[9][17]=1.9;data.dist[9][18]=1.7;
		data.dist[9][19] = 1.1;data.dist[9][20]=1.7;data.dist[9][21]=2.3;data.dist[9][22]=2.2;data.dist[9][23]=1.6;
		data.dist[9][24] = 2.4;data.dist[9][25]=1.6;data.dist[9][26]=2.4;data.dist[9][27]=4;data.dist[9][28]=0.32;data.dist[9][29]=1.2;
		data.dist[10][10] = 0;data.dist[10][11]=0.54;data.dist[10][12]=2.1;data.dist[10][13]=0.18;data.dist[10][14]=1.7;
		data.dist[10][15] = 1.6;data.dist[10][16]=2.3;data.dist[10][17]=1.6;data.dist[10][18]=2;data.dist[10][19]=0.42;
		data.dist[10][20] = 2;data.dist[10][21]=2;data.dist[10][22]=1.9;data.dist[10][23]=1.3;data.dist[10][24]=2.6;
		data.dist[10][25] = 1.4;data.dist[10][26]=2.1;data.dist[10][27]=3.3;data.dist[10][28]=0.8;data.dist[10][29]=0.42;
		data.dist[11][11] = 0;data.dist[11][12] = 2.1;data.dist[11][13] = 0.63;data.dist[11][14] = 2.2;data.dist[11][15] = 2.2;data.dist[11][16] =2.8;
		data.dist[11][17] = 2.2;data.dist[11][18] = 2.6;data.dist[11][19] =0.38 ;data.dist[11][20] =2.6 ;data.dist[11][21] = 2.6;
		data.dist[11][22] =2.3 ;data.dist[11][23] = 1.9;data.dist[11][24] = 3.1;data.dist[11][25] = 2;data.dist[11][26] = 2.7;
		data.dist[11][27] = 3.3;data.dist[11][28] = 1.4;data.dist[11][29] = 0.87;
		data.dist[12][12] = 0;data.dist[12][13] =1.9 ;data.dist[12][14] = 1.2;data.dist[12][15] = 1.7;data.dist[12][16] = 1.9;
		data.dist[12][17] = 1.1;data.dist[12][18] = 3.7;data.dist[12][19] = 2.1;data.dist[12][20] = 3.7;data.dist[12][21] = 1.5;
		data.dist[12][22] = 1.4;data.dist[12][23] = 1.2;data.dist[12][24] = 2.1;data.dist[12][25] = 1.9;data.dist[12][26] =1.6;
		data.dist[12][27] = 1.9;data.dist[12][28] = 2.5;data.dist[12][29] = 1.8;
		data.dist[13][13] = 0;data.dist[13][14] = 1.6;data.dist[13][15] = 1.5;data.dist[13][16] = 2.2;data.dist[13][17] = 1.5;
		data.dist[13][18] = 2.1;data.dist[13][19] = 0.48;data.dist[13][20] = 2.1;data.dist[13][21] = 1.9;data.dist[13][22] = 1.8;
		data.dist[13][23] = 1.2;data.dist[13][24] = 2.5;data.dist[13][25] = 1.3;data.dist[13][26] = 2;data.dist[13][27] = 3;
		data.dist[13][28] = 0.86;data.dist[13][29] = 0.32;
		data.dist[14][14] = 0;data.dist[14][15] = 1.4;data.dist[14][16] = 1.1;data.dist[14][17] = 0.53;data.dist[14][18] = 3.6;
		data.dist[14][19] = 2.4;data.dist[14][20] = 3.6;data.dist[14][21] = 0.29;data.dist[14][22] = 0.94;data.dist[14][23] = 0.81;
		data.dist[14][24] = 1.8;data.dist[14][25] = 1.9;data.dist[14][26] = 0.36;data.dist[14][27] = 2.8;data.dist[14][28] = 2.4;data.dist[14][29] = 1.9;
		data.dist[15][15] = 0;data.dist[15][16] = 1.3;data.dist[15][17] = 0.72;data.dist[15][18] = 2.9;data.dist[15][19] = 1.8;
		data.dist[15][20] = 3;data.dist[15][21] = 1;data.dist[15][22] = 0.83;data.dist[15][23] = 0.36;data.dist[15][24] = 1.6;
		data.dist[15][25] = 0.83;data.dist[15][26] = 1.1;data.dist[15][27] = 2.7;data.dist[15][28] = 1.6;data.dist[15][29] = 1.2;
		data.dist[16][16] = 0;data.dist[16][17] = 0.99;data.dist[16][18] = 2.9;data.dist[16][19] = 2.9;data.dist[16][20] = 3;
		data.dist[16][21] = 0.81;data.dist[16][22] = 0.77;data.dist[16][23] = 1.3;data.dist[16][24] = 1.2;data.dist[16][25] = 2;
		data.dist[16][26] = 0.85;data.dist[16][27] = 3.3;data.dist[16][28] = 2.9;data.dist[16][29] = 2.4;
		data.dist[17][17] = 0;data.dist[17][18] = 3.1;data.dist[17][19] = 1.9;data.dist[17][20] = 3.1;data.dist[17][21] = 1.5;
		data.dist[17][22] = 0.56;data.dist[17][23] = 1.3;data.dist[17][24] = 1.3;data.dist[17][25] = 2;data.dist[17][26] = 1.6;
		data.dist[17][27] = 2.4;data.dist[17][28] = 2.8;data.dist[17][29] = 1.4;
		data.dist[18][18] = 0;data.dist[18][19] = 3;data.dist[18][20] = 1.4;data.dist[18][21] = 3.4;data.dist[18][22] = 2.7;
		data.dist[18][23] = 2.7;data.dist[18][24] = 2.8;data.dist[18][25] = 2;data.dist[18][26] = 3.5;data.dist[18][27] = 5.1;
		data.dist[18][28] = 2.2;data.dist[18][29] = 3;
		data.dist[19][19] = 0;data.dist[19][20] = 2.3;data.dist[19][21] = 2.3;data.dist[19][22] = 2.2;data.dist[19][23] = 1.6;
		data.dist[19][24] = 2.9;data.dist[19][25] = 1.7;data.dist[19][26] = 2.4;data.dist[19][27] = 3.6;data.dist[19][28] = 1.1;
		data.dist[19][29] = 0.73;
		data.dist[20][20] = 0;data.dist[20][21] = 3.6;data.dist[20][22] = 2.9;data.dist[20][23] = 2.9;data.dist[20][24] = 3.1;
		data.dist[20][25] = 2.3;data.dist[20][26] = 3.7;data.dist[20][27] = 5.2;data.dist[20][28] = 1.6;data.dist[20][29] = 2.5;
		data.dist[21][21] = 0;data.dist[21][22] = 1.2;data.dist[21][23] = 1;data.dist[21][24] = 2;data.dist[21][25] = 1.7;
		data.dist[21][26] = 1.2;data.dist[21][27] = 2.3;data.dist[21][28] = 2.5;data.dist[21][29] = 1.5;
		data.dist[22][22] = 0;data.dist[22][23] = 1.4;data.dist[22][24] = 0.88;data.dist[22][25] = 2.1;data.dist[22][26] = 1.4;
		data.dist[22][27] = 3;data.dist[22][28] = 2.5;data.dist[22][9] = 2;
		data.dist[23][23] = 0;data.dist[23][24] = 1.4;data.dist[23][25] = 1.1;data.dist[23][26] = 0.77;data.dist[23][27] = 2.1;
		data.dist[23][28] = 1.9;data.dist[23][29] = 1.2;
		data.dist[24][24] = 0;data.dist[24][25] =1.5 ;data.dist[24][26] = 1.7;data.dist[24][27] = 3.4;data.dist[24][28] = 2.5;dist[24][29] = 2.4;
		data.dist[25][25]=0;data.dist[25][26]=1.6;data.dist[25][27]=3.2;data.dist[25][28]=1.4;data.dist[25][29]=1.1;
		data.dist[26][26] = 0;data.dist[26][27] = 2.3;data.dist[26][28] = 2;data.dist[26][29] = 1.4;
		data.dist[27][27] = 0;data.dist[27][28] = 3;data.dist[27][29] = 0.77;
		data.dist[28][28] = 0;data.dist[28][29] = 0.77;
		data.dist[29][29] = 0;
		for (int i = 0; i < data.vertex_num; i++) {
			for (int j = 0; j < data.vertex_num; j++) {
				data.dist[j][i] = data.dist[i][j];
			}
		}

		data.dist[0][data.vertex_num-1] = 0;
		data.dist[data.vertex_num-1][0] = 0;
		//距离矩阵满足三角关系
//		for (int  k = 0; k < data.vertex_num; k++) {
//			for (int i = 0; i < data.vertex_num; i++) {
//				for (int j = 0; j < data.vertex_num; j++) {
//					if (data.dist[i][j] > data.dist[i][k] + data.dist[k][j]) {
//						data.dist[i][j] = data.dist[i][k] + data.dist[k][j];
//					}
//				}
//			}
//		}
		//初始化为完全图
		for (int i = 0; i < data.vertex_num; i++) {
			for (int j = 0; j < data.vertex_num; j++) {
				if (i != j) {
					data.arcs[i][j] = 1;
				}
				else {
					data.arcs[i][j] = 0;
				}
			}
		}
		//除去不符合时间窗和容量约束的边
		for (int i = 0; i < data.vertex_num; i++) {
			for (int j = 0; j < data.vertex_num; j++) {
				if (i == j) {
					continue;
				}
				if (data.a[i]+data.s[i]+data.dist[i][j]+2>data.b[j] ||//这里+2
						data.demands[i]+data.demands[j]>data.cap) {
					data.arcs[i][j] = 0;
				}
				if (data.a[0]+data.s[i]+data.dist[0][i]+data.dist[i][data.vertex_num-1]+2>//这里+2
				data.b[data.vertex_num-1]) {
					System.out.println("the calculating example is false");
					
				}
			}
		}
		for (int i = 1; i < data.vertex_num-1; i++) {
			if (data.b[i] - data.dist[0][i] < min1) {
				min1 = data.b[i] - data.dist[0][i];
			}
			if (data.a[i] + data.s[i] + data.dist[i][data.vertex_num-1] < min2) {
				min2 = data.a[i] + data.s[i] + data.dist[i][data.vertex_num-1];
			}
		}
		if (data.E > min1 || data.L < min2) {
			System.out.println("Duration false!");
			System.exit(0);//终止程序
		}
		//初始化配送中心0，n+1两点的参数
		data.arcs[data.vertex_num-1][0] = 0;
		data.arcs[0][data.vertex_num-1] = 1;
		for (int i = 1; i < data.vertex_num-1; i++) {
			data.arcs[data.vertex_num-1][i] = 0;
		}
		for (int i = 1; i < data.vertex_num-1; i++) {
			data.arcs[i][0] = 0;
		}
	}
}