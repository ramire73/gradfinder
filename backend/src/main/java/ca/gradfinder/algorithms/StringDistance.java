package ca.gradfinder.algorithms;

/** How different two words are; 0 means equal. SpellChecker works with any implementation (strategy pattern). */
public interface StringDistance {
    int between(String a, String b);
}
