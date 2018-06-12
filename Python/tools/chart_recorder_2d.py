#!/usr/bin/env python

# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
#                   Simulation with reactive streams                    #
# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #


"""2D chart recorder tool
Plotting of points and react on keyboard input
Created 09.06.2018 author CAB
"""

from typing import Callable, List, Tuple
import matplotlib.pyplot as plt

# Definitions
class ChartRecorder2D:
    '''
    Plotting of points and react on keyboard input.
    '''

    def __init__(
            self,
            name: str,
            lines: List[Tuple[str, str]],
            x_range: Tuple[float, float] = None,
            y_range: Tuple[float, float] = None,
            xy_label: Tuple[str, str] = ("X", "Y"),
            pause: float = .01,
            window_size: Tuple[float, float] = (10, 6)):
        '''
        Construct a empty plot wit given number of lines
        :param lines: list of lines [(<line name>, <matplotlib format>)]
        :param x_range: the optional range of X axis, if None will auto scaled
        :param y_range: the optional range of Y axis, if None will auto scaled
        :param pause: execution timeout (should not be to small since UI will frozen)
        :param window_size: Size of the window (w, h), in inches (1in == 2.54cm)
        '''
        # Parameters
        self.__window_title = "Chart Recorder 2D"
        self.__mim_min = -.1
        self.__max_max = +.1
        self.__font_size=12
        self.__font_size=12
        self.__padding_left=.1
        self.__padding_bottom=.09
        self.__padding_right=.97
        self.__padding_top=.95
        # Fields
        self.__xs = []
        self.__yss = []
        self.__plots = []
        self.__callbacks = []
        self.__x_range = x_range
        self.__y_range = y_range
        self.__pause = pause
        # Init
        fig, self.__ax = plt.subplots()
        self.__ax.grid(color="gray")
        fig.subplots_adjust(self.__padding_left, self.__padding_bottom, self.__padding_right, self.__padding_top)
        fig.canvas.set_window_title(self.__window_title)
        self.__ax.set_title(name, fontsize=self.__font_size)
        if x_range is not None:
            self.__ax.set_xlim(x_range[0], x_range[1])
        else:
            self.__ax.set_xlim(-1, +1)
        if y_range is not None:
            self.__ax.set_ylim(y_range[0], y_range[1])
        else:
            self.__ax.set_ylim(-1, +1)
        self.__ax.set_xlabel(xy_label[0], fontsize=self.__font_size)
        self.__ax.set_ylabel(xy_label[1], fontsize=self.__font_size)
        fig.set_size_inches(*window_size)
        # Build plots
        for name, form in lines:
            ys = []
            p, = self.__ax.plot(self.__xs, ys, form, label=name)
            self.__plots.append(p)
            self.__yss.append(ys)
        fig.legend()
        # Set keyboard handler
        fig.canvas.mpl_connect(
            'key_release_event',
            lambda event: [callback(event.key) for callback in self.__callbacks])
        # Show plot
        plt.pause(0.001)

    def append(self, x: float, ys: [float]) -> None:
        '''
        Append point fot each Y coordinate at X coordinate
        :param x: X coordinate
        :param ys: list of Y coordinated
        '''
        # Functions
        def min_max(vs):
            mnv = min(vs)
            mxv = max(vs)
            if mnv == mxv:
                return mnv + self.__mim_min, mxv + self.__max_max
            else:
                return mnv, mxv
        # Add new points
        self.__xs.append(x)
        for plot, ys, y in zip(self.__plots, self.__yss, ys):
            ys.append(y)
            plot.set_data(self.__xs, ys)
        # Set X range
        if self.__x_range is None:
            self.__ax.set_xlim(*min_max(self.__xs))
        # Set Y range
        if self.__y_range is None:
            min_y = self.__mim_min
            max_y = self.__max_max
            for ys in self.__yss:
                mn, mx = min_max(ys)
                if mn < min_y: min_y = mn
                if mx > max_y: max_y = mx
            self.__ax.set_ylim(min_y, max_y)
        # Update plot
        plt.pause(self.__pause)

    def on_kay_press(self, handler: Callable[[str], None]) -> None:
        '''
        Will run given handler on keyboard key pressed and pass key name
        :param handler: callback <key name> -> None
        '''
        self.__callbacks.append(handler)

    def show(self) -> None:
        '''
        Used to make chart visible after program ended
        '''
        plt.show()
