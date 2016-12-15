package neuralnet.test.tictactoe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class TicTacToe {
	
	public int[] board;
	
	public boolean hasWinner;
	
	public int winnerID;
	
	public static ArrayList<TicTacToe> generateBoards(int numBoards, int dim) {
		DIM = dim;
		ArrayList<TicTacToe> list = new ArrayList<TicTacToe>();
		for(int i=0; i<numBoards; i++) {
			list.add(getRandomBoard());
		}
		return list;
	}
	
	private static ArrayList<Integer> ids;
	private static int DIM = 3;
	
	private static TicTacToe getRandomBoard() {
		if(ids == null) {
			ids = new ArrayList<Integer>();
			for(int i=0; i<DIM*DIM; i++) {
				ids.add(i);
			}
		}
		TicTacToe t = new TicTacToe();
		t.fillBoardAndSetWinner();
		return t;
	}

	private void fillBoardAndSetWinner() {
		// Select who goes first (to balance data)
		int firstPlayer = (Math.random() > 0.5) ? 0 : 1;
		// Fill board
		this.board = new int[DIM*DIM];
		this.winnerID = 0;
		Arrays.fill(this.board, 0);
		Collections.shuffle(ids);
		for(int i=0; i<ids.size(); i++) {
			int cell = ids.get(i);
			this.board[cell] = ((i%2==firstPlayer) ? 1 : -1);
			if(this.wonOnMove(cell)) {
				this.winnerID = (this.board[cell]==1) ? 1 : 2;
				this.hasWinner = true;
				break;
			}
		}
	}

	private boolean wonOnMove(int cell) {
		int player = this.board[cell];
		// Check row
		int R = cell/DIM;
		boolean rowWins = true;
		for(int c=0; rowWins && c<DIM; c++) {
			rowWins = rowWins && this.board[R+c] == player;
		}
		if(rowWins) {
			return true;
		}
		// Check column
		int C = cell%DIM;
		boolean colWins = true;
		for(int r=0; colWins && r<DIM; r++) {
			R = r*DIM;
			colWins = colWins && this.board[R+C] == player;				
		}
		if(colWins) {
			return true;
		}
		// Check diagonals
		boolean diagWins = true;
		for(int i=0; i<DIM; i++) {
			if(this.board[i*DIM+i] != player) {
				diagWins = false;
				break;
			}
		}
		if(diagWins) {
			return true;
		}
		diagWins = true;
		for(int i=0; i<DIM; i++) {
			if(this.board[i*DIM+(DIM-i-1)] != player) {
				diagWins = false;
				break;
			}
		}
		if(diagWins) {
			this.hasWinner = true;
		}	
		return false;
	}

	public int getWinnerID() {
		/**
		 * 0: no winner
		 * 1: positive (X) wins
		 * 2: negative (O) wins
		 * 3: both win
		 */
		int id = 0;
		if(checkWinner(1)) {
			id = 1;
		}
		if(checkWinner(-1)) {
			if(id == 1) {
				id = 3;
			}
			else {
				id = 2;
			}
		}
		return id;
	}

	private boolean checkWinner(int player) {		
		for(int r=0; r<DIM; r++) {
			int R = r*DIM;
			boolean rowWins = true;
			for(int c=0; c<DIM; c++) {
				rowWins = rowWins && this.board[R+c] == player;
			}
			if(rowWins) {
				return true;
			}
		}
		for(int c=0; c<DIM; c++) {
			boolean colWins = true;
			for(int r=0; r<DIM; r++) {
				int R = r*DIM;
				colWins = colWins && this.board[R+c] == player;				
			}
			if(colWins) {
				return true;
			}
		}
		boolean diagWins = true;
		for(int i=0; i<DIM; i++) {
			if(this.board[i*DIM+i] != player) {
				diagWins = false;
				break;
			}
		}
		if(diagWins) {
			return true;
		}
		diagWins = true;
		for(int i=0; i<DIM; i++) {
			if(this.board[i*DIM+(DIM-i-1)] != player) {
				diagWins = false;
				break;
			}
		}
		if(diagWins) {
			this.hasWinner = true;
		}	
		return false;
	}

	public String toString() {
		String win = "None";
		if(this.winnerID == 1) {
			win = "X";
		}
		if(this.winnerID == 2) {
			win = "O";
		}
		String ret = "Winner: "+win+"\n";
		for(int r=0; r<DIM; r++) {
			for(int c=0; c<DIM; c++) {
				ret += pp(this.board[r*DIM + c])+" ";
			}
			ret += "\n";
		}
		return ret;
	}

	private String pp(int i) {
		if(i==1) {
			return "X";
		}
		if(i==-1) {
			return "O";
		}
		return ".";
	}
}
