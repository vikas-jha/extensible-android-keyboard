package com.example.extkeyboard;

public enum EBoardTypes {
	Numeric, // Keypad, numeric keyboard
	Symbol,	// Symbols keyboard
	
	//Custom input
	Custom, // Emoji, Handwriting etc
    Emoji,
	
	//Language type enums
	Latin,		// Alphabetic 	- consonant or vowel -  English, French, Roman 
	Indic,		// Abugida 		- consonant + vowel - Indian Languages - Hindi, Bangla, Kannada
	Arabic,		// Abjad RTL 	- consonant [vowel]
	Kanji,		// Logographic 	- Chinese
	Kana,		// Syllabic		- [consonant + vowel] - Japanese Harigana, Katakana
	Hangul		// Featural     - Korean hangul
	
}
