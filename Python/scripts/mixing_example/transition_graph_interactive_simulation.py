#!/usr/bin/env python

# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
#                   Simulation with reactive streams                    #
# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #

"""Interactive simulation with transition graph representation of mixing problem
onenote:https://d.docs.live.net/8e55450f976ac566/Notes/AI/Статьи.one#SWRS%20v0.2.3%20Graph%20modeling&
    section-id={40964F6B-F1E6-40E5-93D6-D7D464B4D57F}&page-id={BB7332E8-6019-4F25-8EB8-970FBE79BC2F}&
    object-id={AB21A1CE-24D1-09BB-1213-C2C15F1AAB66}&22
Created 24.07.2018 author CAB
"""

import numpy as np
import matplotlib.pyplot as plt
import math as m
from tools.graph_visualisation import NodeLike, GraphLike, GraphVisualisation
from tools.chart_recorder_2d import ChartRecorder2D


# Script init
print(""" 
#### Interactive simulation with transition graph representation of mixing problem ####
X = [t]
Y = [ω_1, ω_2, ω_3̂]
G = [v_1, v_2, q_1, q_2, q_3, q_4]
""")

# Value definitions
class b𝔛_:
    def __init__(self, t):
        self.t = t
    def __repr__(self):
        return f"𝔛 = [t={self.t}]"
class h𝔜_:
    def __init__(self, ω_1, ω_2, ω_3):
        self.ω_1 = ω_1
        self.ω_2 = ω_2
        self.ω_3 = ω_3
    def __repr__(self):
        return f"𝔜 = [ω_1={self.ω_1}, ω_2={self.ω_2}]"
class 𝔈_:
    def __init__(self, v_1, v_2, q_1, q_2, q_3, q_4):
        self.v_1 = v_1
        self.v_2 = v_2
        self.q_1 = q_1
        self.q_2 = q_2
        self.q_3 = q_3
        self.q_4 = q_4
    def __repr__(self):
        return f"𝔈 = [v_1={self.v_1}, v_2={self.v_2}, q_1={self.q_1}, q_2={self.q_2}, q_3={self.q_3}, q_4={self.q_4}]"
class 𝔓_h_:
    def __init__(self, 𝔈, h):
        if h == 1:
            self.v_1 = 𝔈.v_1
            self.q_1 = 𝔈.q_1
            self.q_2 = 𝔈.q_2
            self.q_3 = 𝔈.q_3
        elif h == 2:
            self.v_2 = 𝔈.v_2
            self.q_2 = 𝔈.q_2
            self.q_3 = 𝔈.q_3
            self.q_4 = 𝔈.q_4
        else:
            assert False, "index 'h' can be 1 or 2"
        self.h = h
    def __repr__(self):
        if self.h == 1:
            return f"𝔓_h = [v_1={self.v_1}, q_1={self.q_1}, q_2={self.q_2}, q_3={self.q_3}]_h=1"
        else:
            return f"𝔓_h = [v_2={self.v_2}, q_2={self.q_2}, q_3={self.q_3}, q_4={self.q_4}]_h=2"

class 𝔖𝔛_q_: # Sub-state, 𝔖 ⊆ 𝔜 is sub-stat value, 𝔛 ∈ 𝕏 is key values set, q ∈ ℕ is sub-sate index
    def __init__(self, t, ω, q):
        self.t = t
        if q == 1:
            self.ω_1 = ω
        elif q == 2:
            self.ω_2 = ω
        elif q == 3:
            self.ω_3 = ω
        else:
            assert False, "index q can be only 1 or 2 or 3"
        self.q = q
    def __repr__(self):
        if self.q == 1:
            return f"𝔖^𝔛_q = 𝔖=[ω_1={self.ω_1}]^𝔛=[t={self.t}]_q=1"
        elif self.q == 2:
            return f"𝔖^𝔛_q = 𝔖=[ω_2={self.ω_2}]^𝔛=[t={self.t}]_q=2"
        else:
            return f"𝔖^𝔛_q = 𝔖=[ω_3={self.ω_3}]^𝔛=[t={self.t}]_q=3"

# Node definitions
class S_node(NodeLike): # Holds sub-state 𝔖𝔛q, d, w is index of S_node in the Γ graph
    def __init__(self, d, w):
        self.d = d
        self.w = w
        self.__S = None
    def is_defined(self):
        return self.__S is not None
    def assign(self, 𝔖𝔛q):
        assert 𝔖𝔛q is not None
        assert 𝔖𝔛q.q == self.w
        self.__S = 𝔖𝔛q
    def get(self):
        return self.__S
    def graph_repr(self):
        label = f"S_{self.d},{self.w}"
        pos = (self.d, self.w)
        color = "k" if self.is_defined() else "m"
        return label, pos, color, []
    def __repr__(self):
        if self.__S is None:
            return f"S_d,w = (∅)_d={self.d},w={self.w}"
        else:
            return f"S_d,w = ({self.__S})_d={self.d},w={self.w}"
class Θ𝔓_node(NodeLike): # A transaction function
    def __init__(self, S_1, S_2, S_3, 𝔓_h, gS, d, w, f_ω):
        assert S_1.d == (d - 1)
        assert S_2.d == (d - 1)
        assert S_3.d == (d - 1)
        assert gS.d == d
        assert S_1.w == 1
        assert S_2.w == 2
        assert S_3.w == 3
        assert gS.w == w
        assert 𝔓_h.h == w
        self.d = d
        self.w = w
        self.__S_1 = S_1
        self.__S_2 = S_2
        self.__S_3 = S_3
        self.__𝔓_h  = 𝔓_h
        self.__gS = gS
        self.__f_ω = f_ω
    def eval(self):
        doEval = \
            self.__S_1.is_defined() and \
            self.__S_2.is_defined() and \
            self.__S_3.is_defined() and not \
            self.__gS.is_defined()
        if doEval:
            assert self.__S_1.get().t == self.__S_2.get().t
            ω_1 = self.__S_1.get().ω_1
            ω_2 = self.__S_2.get().ω_2
            ω_3 = self.__S_3.get().ω_3
            t_prev = self.__S_1.get().t
            t_next = self.__S_3.get().t
            ω = self.__f_ω(ω_1, ω_2, ω_3, t_prev, t_next, self.__𝔓_h)
            𝔖𝔛_q = 𝔖𝔛_q_(t_next, ω, q=self.w)
            self.__gS.assign(𝔖𝔛_q)
            return 𝔖𝔛_q
    def graph_repr(self):
        label = f"Θ_{self.d},{self.w}"
        pos = (self.d - .3, self.w + (.2 if self.w == 1 else -.2))
        color = "g"
        edges = [(self.__S_1, self), (self.__S_2, self), (self.__S_3, self), (self, self.__gS)]
        return label, pos, color, edges
    def __repr__(self):
        return f"{self.__S_1}, {self.__S_2}, {self.__S_3} --> Θ^|𝔓_d={self.d},w={self.w} --> {self.__gS}"

# Graph definitions
class Γ𝔈_graph(GraphLike):
    def __init__(self, setS, setΘ_𝔓, set𝔓):
        for S in setS:
            assert not S.is_defined()
        self.__setS = setS
        self.__setΘ_𝔓 = setΘ_𝔓
        self.__set𝔓 = set𝔓
    def init(self, p𝔖):
        for (d, w), 𝔖𝔛_q in p𝔖.items():
            for S in self.__setS:
                if S.d == d and S.w == w:
                    S.assign(𝔖𝔛_q)
    def eval(self):
        do_eval = True
        set𝔖𝔛_q = [] # Set of 𝔖^𝔛_q evolved in this iteration
        while do_eval:
            do_eval = False
            for Θ_𝔓 in self.__setΘ_𝔓:
                𝔖𝔛_q = Θ_𝔓.eval()
                if 𝔖𝔛_q is not None:
                    do_eval = True
                    set𝔖𝔛_q.append(𝔖𝔛_q)
                    self.redraw()
        return set𝔖𝔛_q
    def graph_repr(self):
        return self.__setS + self.__setΘ_𝔓
    def __repr__(self):
        rs = "Γ^|𝔈: \n"
        for Θ_𝔓 in self.__setΘ_𝔓: rs = rs + f"    {str(Θ_𝔓)}\n"
        return rs

# Γ^|𝔈 builder
def build_Γ𝔈(n, 𝔈, S_transition):
    _𝔓_1 = 𝔓_h_(𝔈, h=1)
    _𝔓_2 = 𝔓_h_(𝔈, h=2)
    f_ω_1, f_ω_2 = S_transition()
    S_1 = S_node(d=0, w=1)
    S_2 = S_node(d=0, w=2)
    setS = [S_1, S_2]
    setS_3 = []
    setΘ_𝔓 = []
    for i in range(1, n + 1):
        S_3 = S_node(d=i-1, w=3)
        gS_1 = S_node(d=i, w=1)
        gS_2 = S_node(d=i, w=2)
        Θ_𝔓_1 = Θ𝔓_node(S_1, S_2, S_3, _𝔓_1, gS_1, d=i, w=1, f_ω=f_ω_1)
        Θ_𝔓_2 = Θ𝔓_node(S_1, S_2, S_3, _𝔓_2, gS_2, d=i, w=2, f_ω=f_ω_2)
        setS.extend([S_3, gS_1, gS_2])
        setΘ_𝔓.extend([Θ_𝔓_1, Θ_𝔓_2])
        setS_3.append(S_3)
        S_1 = gS_1
        S_2 = gS_2
    set𝔓 = [_𝔓_1, _𝔓_2]
    return Γ𝔈_graph(setS, setΘ_𝔓, set𝔓), setS_3

# Parameters
𝔈 = 𝔈_(
     v_1 = 4,  # L
     v_2 = 8,  # L
     q_1 = 3,  # L/m
     q_2 = 2,  # L/m
     q_3 = 5,  # L/m
     q_4 = 3)  # L/m
n = 100
Δt = .1
p𝔖 = {
    (0,1): 𝔖𝔛_q_(t=.0, ω=0, q=1),    #State for S_d=0,w=1
    (0,2): 𝔖𝔛_q_(t=.0, ω=20, q=2)}   #State for S_d=0,w=2
init_ω_3 = 10.0
up_down_step = 1

# Transition implementation
def S_transition():
    def f_ω_1(ω_1, ω_2, ω_3, t, t_next, 𝔓_1):
        return ω_1 + (t_next - t) * (((𝔓_1.q_1 * ω_3) + (𝔓_1.q_2 * ω_2) - (𝔓_1.q_3 * ω_1)) / 𝔓_1.v_1)
    def f_ω_2(ω_1, ω_2, ω_3, t, t_next, 𝔓_2):
        return ω_2 + (t_next - t) * (((𝔓_2.q_3 * ω_1) - (𝔓_2.q_2 * ω_2) - (𝔓_2.q_4 * ω_2)) / 𝔓_2.v_2)
    return f_ω_1, f_ω_2

# Build Γ^|𝔈
Γ𝔈, setS_3 = build_Γ𝔈(n, 𝔈, S_transition)
print(Γ𝔈)
graph_viz = GraphVisualisation("Γ_graph", Γ𝔈, pause=.05)

# Chart
chart = ChartRecorder2D(
    "Simulation for ω_1 and ω_1 with variable ω_3",
    lines=[("ω_1", "g"), ("ω_2", "r"), ("ω_3", "b--")],
    y_range=(0, 20),
    x_range=(0, 10),
    pause=.05)

# Helpers functions
class Helpers:
    def __init__(self, init_ω_3, up_down_step, chart):
        self.__input = ""
        self.__ω_3 = init_ω_3
        self.__up_down_step = up_down_step
        self.__chart = chart
        def on_key(key):
            print(f"Pressed key = {key}")
            self.__input = key
        chart.on_kay_press(on_key)
    def not_terminated(self):
        if self.__input == "e":
            self.__input = ""
            print("Program ended!")
            return False
        else:
            return True
    def get_ω_3(self):
        if self.__input == "up":
            self.__ω_3 += self.__up_down_step
            self.__input = ""
        if self.__input == "down":
            self.__ω_3 -= self.__up_down_step
            self.__input = ""
        return self.__ω_3
    def show(self, X, Y):
        print(f"X = {X}, Y = {Y}")
        self.__chart.append(x = X.t, ys = [Y.ω_1, Y.ω_2, Y.ω_3])
H = Helpers(init_ω_3, up_down_step, chart)

# Interactive simulation
Γ𝔈.init(p𝔖)
i = 0
while H.not_terminated() and i < n:
    S_3 = setS_3[i]
    assert S_3.d == i
    assert S_3.w == 3
    t = i * Δt
    ω_3 = H.get_ω_3()
    S_3.assign(𝔖𝔛_q_(t, ω_3, q=3))
    graph_viz.update()
    set𝔖𝔛_q = Γ𝔈.eval()
    assert len(set𝔖𝔛_q) == 2, f"Not all 𝔖𝔛_q evaluated, set𝔖𝔛_q = {set𝔖𝔛_q}"
    𝔖𝔛_1 = None
    𝔖𝔛_2 = None
    for 𝔖𝔛_q in set𝔖𝔛_q:
        if 𝔖𝔛_q.q == 1:
            𝔖𝔛_1 = 𝔖𝔛_q
        if 𝔖𝔛_q.q == 2:
            𝔖𝔛_2 = 𝔖𝔛_q
    X = b𝔛_(t)
    Y = h𝔜_(𝔖𝔛_1.ω_1, 𝔖𝔛_2.ω_2, ω_3)
    H.show(X, Y)
    i += 1

#Show plots
plt.show()
