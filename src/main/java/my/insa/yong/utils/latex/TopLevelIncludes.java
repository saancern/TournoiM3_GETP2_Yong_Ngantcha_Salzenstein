package my.insa.yong.utils.latex;

import java.util.ArrayList;
import java.util.List;

/**
 * un ensemble d'includes à ajouter en entete d'un fichier latex.
 * Pour l'instant une implémentation vraiment basique sous forme de liste
 * de String.
 * l'identité de deux top-levels est testé sous forme de l'égalité des strings.
 * Donc très sensible : un espace supplémentaire, et il n'y a plus d'égalité.
 * De plus, on estime que cette liste ne va pas devenir très grande. Donc,
 * lors d'un add, on teste basiquement l'existence dans la liste, sans TreeSet
 * ou autre qui permettrait d'optimiser ce test d'existence.
 * A priori, devra être utilisé en mode préfix lors des collect : 
 * on inclu d'abord les includes nécessaires à la construction principale,
 * puis les includes des sous-constructions.
 * @author saancern
 */
public class TopLevelIncludes {
    
    private List<String> includes;
    
    public TopLevelIncludes() {
        this.includes = new ArrayList<>();
    }
    
    public boolean add(String include) {
        if (this.includes.contains(include)) {
            return false;
        } else {
            this.includes.add(include);
            return true;
        }
    }
    
    public String toLatex() {
        return this.includes.stream().map(s -> s + "\n").reduce("", String::concat);
    }
    
}
