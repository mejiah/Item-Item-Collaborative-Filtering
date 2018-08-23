package homework2;

import java.awt.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.CollectionCertStoreParameters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

// Humberto Mejia
// Data Mining 315
// HW 2: Item-Item Collaborative Filtering
public class HW2 {
	
	public static final int COL = 671; // 12 // 671
	public static final int ROW = 9125; // 6 // 9125
	public static final int TOP = 5; // 2 // 5
	
	public static double[][] profile = new double[ROW][COL];
	public static double[][] recommendations = new double[ROW][COL];
	public static double[][] weightedValues = new double[ROW][COL];
	public static  Map<Integer, Integer> MovieIdToArrayIndex = new LinkedHashMap<Integer, Integer>();
	public static  Map<Integer, Integer> ArrayIndexToMovieId = new LinkedHashMap<Integer, Integer>();
	public static ArrayList<ArrayList<String>> topNeighbors = new ArrayList<ArrayList<String>>();
	public static ArrayList<ArrayList<String>> topRecommendations = new ArrayList<ArrayList<String>>();
	public static ArrayList<Double> ratingsSums = new ArrayList<Double>();
	public static ArrayList<Double> weightedSums = new ArrayList<Double>();
	public static double[][] cosineValues = new double[ROW][ROW];
	
	public static void main(String args[]) throws IOException {
		
		indexMovieIds();
		processData();
		calcWeightedValues();
		calcCenterCosine();
		calcTemp();		
		getPredictions();		
		getTopFiveRecommendations();
		
		// print results to outfile.txt
		PrintWriter writer = new PrintWriter("./output.txt", "UTF-8");		
		int index = 1;
		for (ArrayList<String> user : topRecommendations) {
			writer.print(index + " ");
			for (String recommendation : user) {
				writer.print(ArrayIndexToMovieId.get(Integer.parseInt(recommendation.split(",")[1])) + " ");
			}
			index++;
			writer.println();
		}
		writer.close();
	}
	
	// get movie data, store in HashMap
	public static void indexMovieIds() throws IOException {
		File file = new File("./movies.csv");
		BufferedReader br = new BufferedReader(new FileReader(file));
		
	    String line = null;
		String data[];
		int count = 0;
		line = br.readLine();
	    while ((line = br.readLine()) != null) {
	    	data = line.split(",");
	    	MovieIdToArrayIndex.put(Integer.parseInt(data[0]), count); 
	    	ArrayIndexToMovieId.put(count, Integer.parseInt(data[0])); // to access movie by index
	    	count++;    	
	    }
	    br.close();
	}
	
	// get ratings data
	public static void processData() throws IOException {
		
		File file = new File("./ratings.csv");
		BufferedReader br = new BufferedReader(new FileReader(file));
		
	    String line = null;
		String data[];
		line = br.readLine();
	    while ((line = br.readLine()) != null) {
	    	data = line.split(",");
	    	mapData(Integer.parseInt(data[0]), Integer.parseInt(data[1]), Double.parseDouble(data[2]));
	    	
	    }
	    br.close();
	}
	
	// map users, ratings, and movie data together
	public static void mapData(int userId, int movieId, double rating) {
		
		if (MovieIdToArrayIndex.containsKey(movieId)) {
			profile[MovieIdToArrayIndex.get(movieId)][userId - 1] = rating;
		} else {
			System.out.println("Movie Not Found");
		}
	}
	
	// calculate the weghted values for movie ratings
	public static void calcWeightedValues() {
		double sumRatings = 0;
		int numRatings = 0;
		for (int row = 0; row < ROW; row++) {
			for (int col = 0; col < COL; col++) {
				sumRatings += profile[row][col];
				if (profile[row][col] > 0) {
					numRatings++;
				}
			}
			for (int col = 0; col < COL; col++) {
				
				if (profile[row][col] > 0) {
					weightedValues[row][col] = profile[row][col] - (sumRatings / (double)numRatings);
				} else {
					weightedValues[row][col] = 0.0;
				}
				
			}
			numRatings = 0;
			sumRatings = 0;
		}
	}
	
	// calculate center cosine values
	public static void calcCenterCosine() {
		for (int i = 0; i < ROW; i++) {
			for (int j = i + 1; j < ROW; j++) {
				cosineValues[i][j] = calcCenterCosine(weightedValues[i], weightedValues[j]);
			}
		}
	}
	
	// override, helper function
	private static double calcCenterCosine(double a[], double b[]) {
		
		double sum = 0;
		double sumSquareA = 0;
		double sumSquareB = 0;
		for (int i = 0; i < COL; i++) {
			sum += a[i] * b[i];
			sumSquareA += a[i] * a[i];
			sumSquareB += b[i] * b[i];
		}
		if (Math.sqrt(sumSquareA) * Math.sqrt(sumSquareB) != 0.0) {
			return sum / (Math.sqrt(sumSquareA) * Math.sqrt(sumSquareB));
		} else {
			return 0.0;
		}
	}
	
	// get results from processed data
	public static void calcTemp() {
		ArrayList <String> temp = new ArrayList<String>();
		for (int i = 0; i < ROW; i++) {
			for (int j = 0; j < ROW; j++) {
				if (j != i) {
					if (i < j) {
						temp.add(cosineValues[i][j] + "," + ArrayIndexToMovieId.get(j));
					} else if (i > j) {
						temp.add(cosineValues[j][i] + "," + ArrayIndexToMovieId.get(j));
					}
				}
			}
			sortRankings(temp);
			getTopFiveNeighbors(temp);
			
			temp.clear();
		}
	}
	
	
	
	/*public static void calcCenterCosine() {
		
		ArrayList <String> temp = new ArrayList<String>();
		for (int i = 0; i < ROW; i++) {
			for (int j = 0; j < ROW; j++) {
				if (j != i) {
					temp.add(calcCenterCosine(weightedValues[i], weightedValues[j]) + "," + ArrayIndexToMovieId.get(j));
				}
			}
			sortRankings(temp);
			getTopFiveNeighbors(temp);
			
			temp.clear();
		}
	}*/
	
	// get top five neighbors
	public static void getTopFiveNeighbors(ArrayList<String> list) {
		ArrayList<String> temp = new ArrayList<String>();
		for (int i = 0; i < TOP; i++) {
			temp.add(list.get(i));
		}
		topNeighbors.add(temp);
	}
	
	// sort movie-user result data
	public static void sortRankings(ArrayList<String> temp) {
		Collections.sort(temp, Collections.reverseOrder());
		String a[], b[];
		int count = 0;
		for (int k = 0; k < temp.size(); k++) {
			for (int h = k + 1; h < temp.size(); h++) {
				a = temp.get(k).split(",");
				b = temp.get(h).split(",");
				if (a[0].equals(b[0])) {
					count++;
				} else {
					break;
				}
			}
			
			if (count > 0) {
				int  t = k;
				int s = count;
				while (count > 0) {
					String tmp = temp.get(t);
					temp.set(t, temp.get(t + count));
					temp.set(t + count, tmp);
					count-=2;
					t++;
				}
				k = k + s;
				count = 0;
			}	
		}
	}
	
	// get prediction values for movies for all users 
	public static void getPredictions() {
		for (int i = 0; i < ROW; i++) {
			for (int j = 0; j < COL; j++) {
				if (profile[i][j] <= 0) {
					recommendations[i][j] = calcPredictions(i, j); 
				}
			}
		}
	}
	
	// calculate predictions ratings
	public static double calcPredictions(int movieId, int user) { 
		ArrayList<String> temp = topNeighbors.get(movieId);
		String a[];
		Double totalNum = 0.0;
		Double totalDenom = 0.0;
		for (String movie : temp) {
			a = movie.split(",");
			totalNum += profile[MovieIdToArrayIndex.get(Integer.parseInt(a[1]))][user] * Double.parseDouble(a[0]);
			totalDenom += Double.parseDouble(a[0]);
		}
		if (totalDenom != 0) {
			return totalNum / totalDenom;
		} else {
			return 0;
		}	
	}
	
	// get top five recommnedations for each user
	public static void getTopFiveRecommendations() {
		ArrayList<String> temp = new ArrayList<String>();
		ArrayList<String> tempTopVals = new ArrayList<String>();
		for (int i = 0; i < COL; i++) {
			for (int j = 0; j < ROW; j++) {
				temp.add(recommendations[j][i] + "," + j);
			}
			sortRankings(temp);
			for (int k = 0; k < TOP; k++) {
				tempTopVals.add(temp.get(k));
			}
			topRecommendations.add(tempTopVals);
			tempTopVals = new ArrayList<String>();
			temp.clear();
		}
	}
	
}





