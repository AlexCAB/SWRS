#!/usr/bin/env python

# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
#                   Simulation with reactive streams                    #
# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #


"""2D chart recorder tool
Plotting of points and react on keyboard input
Created 09.06.2018 author CAB
"""

from typing import List, Tuple
from types import MethodType
import matplotlib.pyplot as plt
import abc
import networkx as nx

# Definitions
class NodeLike(metaclass=abc.ABCMeta):
    @abc.abstractmethod
    def graph_repr(self) -> Tuple[str, Tuple[int, int], str, List[Tuple['NodeLike', 'NodeLike']]]:
        """
        Used to pool of Node representation by GraphVisualisation
        :return: (displayed label, (x position, y position), color char, list of edges like (this node -> other node)
        """
        pass

class GraphLike(metaclass=abc.ABCMeta):
    @abc.abstractmethod
    def graph_repr(self) -> List[NodeLike]:
        """
        Used to pool of graph representation (all nodes) by GraphVisualisation
        :return: list of all nodes
        """
        pass
    def redraw(self):
        """
        Can be used by Graph implementation to update its visual representation
        :return: None
        """
        pass

class GraphVisualisation:
    '''
    Visualisation of DAG.
    '''

    def __init__(
            self,
            name: str,
            graph: GraphLike,
            pause: float = .01,
            window_size: Tuple[float, float] = (14, 6)):
        '''
        Construct a visualisation for given graph implementation
        :param graph: an graph implementation as list of ones
        :param pause: execution timeout (should not be to small since UI will frozen)
        :param window_size: Size of the window (w, h), in inches (1in == 2.54cm)
        '''
        # Parameters
        self.__window_title = "Graph visualisation: " + name
        self.__xy_label_font_size=12
        self.__padding_left=.05
        self.__padding_bottom=.09
        self.__padding_right=.97
        self.__padding_top=.98
        # Fields
        self.__graph = graph
        self.__pause = pause
        # Init
        fig, self.__ax = plt.subplots()
        self.__graph_view = nx.Graph()
        fig.subplots_adjust(self.__padding_left, self.__padding_bottom, self.__padding_right, self.__padding_top)
        fig.canvas.set_window_title(self.__window_title)
        self.__ax.set_xlabel("depth", fontsize=self.__xy_label_font_size)
        self.__ax.set_ylabel("width", fontsize=self.__xy_label_font_size)
        fig.set_size_inches(*window_size)
        # Set callback
        graph.redraw = MethodType(lambda a: self.__render(pause), self)
        # First render of graph
        self.__render(0.0001)

    def __render(self, t_pause):
        # Build view
        nodes = self.__graph.graph_repr()
        labels = {}
        positions = {}
        edges = []
        colors = ""
        for node in nodes:
            label, pos, color, eds = node.graph_repr()
            assert len(color) == 1, "color string should have exactly 1 char length"
            labels[node] = label
            positions[node] = pos
            colors += color
            edges.extend(eds)
        # Set view
        self.__graph_view.clear()
        self.__ax.clear()
        self.__graph_view.add_nodes_from(nodes)
        self.__graph_view.add_edges_from(edges)
        nx.draw_networkx(self.__graph_view, ax= self.__ax, labels=labels, pos=positions, node_color=colors)
        # Show plot
        plt.pause(t_pause)

    def update(self):
        """
        Re-render graph view
        :return: None
        """
        self.__render(self.__pause)

    def show(self) -> None:
        '''
        Used to make chart visible after program ended
        '''
        plt.show()
