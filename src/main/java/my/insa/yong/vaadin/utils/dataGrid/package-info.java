/**
 * définition d'une Grid vaadin générique permettant d'afficher tout ensemble
 * de donnée représenté par une liste de liste d'Object.
 * <pre>
 * <p> vaadin propose un grand nombre de classes pour définir des sources
 * de données associées aux tables. Elle permettent par exemple de ne charger
 * que les données actuellement affichées, de filtrer ...
 * </p>
 * <p> Malheuresusement, ces classes sont plutôt adaptées à des sources de
 * données Spring ou JPA, et pas vraiment prévues pour des sources plus 
 * "basiques" comme celles retrouvées en utilisant jdbc.
 * </p>
 * <p> Dans ce package, une ligne de donnée est simplement une {@code List<Object>}
 * et les données de la table complète est donc une {@code List<List<Object>>>}.
 * L'avantage est que l'on peut mettre tout type d'objet dans la table.
 * L'inconvénient est que l'on perd la vérification statique des types, et que l'on
 * devra utiliser des cast qui ne seront testés qu'à l'exécution.
 * </p>
 * <p> Une des utilisations est l'affichage sous forme de Grid vaadin d'un
 * ResultSet jdbc quelconque. En fait on donnera plutôt un PreparedStatement
 * qui après exécution doit fournir un ResultSet : cela permet d'exécuter de
 * nouveau le PreparedStatement si l'on veut rafraichir les données.
 * </p>
 * </pre>
 */
package my.insa.yong.vaadin.utils.dataGrid;
