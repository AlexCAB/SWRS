#!/usr/bin/env python

# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
#                   Simulation with reactive streams                    #
# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #

"""Function representation of mixing problem and interactive simulation
onenote:https://d.docs.live.net/8e55450f976ac566/Notes/AI/Статьи.one#SWRS%20v0.2.5%20Basic%20modeling&
    section-id={40964F6B-F1E6-40E5-93D6-D7D464B4D57F}&page-id={AFB5685E-E61D-4707-B829-BABB190BC41D}&
    object-id={489EDFFB-40E0-073C-3F49-7F005EDECE76}&64
Created 27.04.2018 author CAB
"""

from tools.chart_recorder_2d import ChartRecorder2D


# Script init
print(""" 
#### Function representation of mixing problem and interactive simulation ####
X = [t]
Y = [ω_1, ω_2̂]
G = [v_1, v_2, q_1, q_2, q_3, q_4, ω_3]
Control keys: up -> ω_3 + 1, down -> ω_3 - 1, e -> exit
""")

# Definitions
class 𝔛_:
    def __init__(self, t):
        self.t = t
    def __repr__(self):
        return f"𝔛 = [t = {self.t}]"
class 𝔜_:
    def __init__(self, ω_1, ω_2):
        self.ω_1 = ω_1
        self.ω_2 = ω_2
    def __repr__(self):
        return f"𝔜 = [ω_1 = {self.ω_1}, ω_2 = {self.ω_2}]"
class 𝔈_:
    def __init__(self, v_1, v_2, q_1, q_2, q_3, q_4, ω_3):
        self.v_1 = v_1
        self.v_2 = v_2
        self.q_1 = q_1
        self.q_2 = q_2
        self.q_3 = q_3
        self.q_4 = q_4
        self.ω_3 = ω_3
    def __repr__(self):
        return f"𝔈 = [v_1 = {self.v_1}, v_2 = {self.v_2}, q_1 = {self.q_1}, q_2 = {self.q_2}, " \
             + f"q_3 = {self.q_3}, q_4 = {self.q_4}, ω_3 = {self.ω_3}]"
    def __eq__(self, other):
        return self.__dict__ == other.__dict__

# Parameters
Δt = .1
𝔛_0 = 𝔛_(t=.0)
𝔜_0 = 𝔜_(ω_1=.0, ω_2=20.0)
𝔈_0 = 𝔈_(
     v_1 = 4,  # L
     v_2 = 8,  # L
     q_1 = 3,  # L/m
     q_2 = 2,  # L/m
     q_3 = 5,  # L/m
     q_4 = 3,  # L/m
     ω_3 = 10) # g/l
up_down_step = 1

# Model (Earlier)
def F(Δt, 𝔛_0, 𝔜_0):
    def eval(X, 𝔈):
        t = 𝔛_0.t
        ω_1 = 𝔜_0.ω_1
        ω_2 = 𝔜_0.ω_2
        while t <= X.t:
            ω_1_m1 = ω_1
            ω_2_m1 = ω_2
            ω_1 = ω_1_m1 + Δt * (((𝔈.q_1 * 𝔈.ω_3) + (𝔈.q_2 * ω_2_m1) - (𝔈.q_3 * ω_1_m1)) / 𝔈.v_1)
            ω_2 = ω_2 + Δt * (((𝔈.q_3 * ω_1_m1) - (𝔈.q_2 * ω_2_m1) - (𝔈.q_4 * ω_2_m1)) / 𝔈.v_2)
            t += Δt
        return 𝔜_(ω_1, ω_2)
    return eval

# Simulations
def simulation(M, setX, 𝔈):
    set𝔜 = []
    for 𝔛 in setX:
        𝔜 = M(𝔛, 𝔈)
        set𝔜.append(𝔜)
    return set𝔜

# Chart
chart = ChartRecorder2D(
    "Simulation for ω_1 and ω_1 with variable ω_3",
    lines=[("ω_1", "g"), ("ω_2", "r"), ("ω_3", "b--")],
    y_range=(0, 20),
    x_range=(0, 30),
    pause=.05)

# Helpers functions
class Helpers:
    def __init__(self, 𝔈_0, 𝔛_0, Δt, chart):
        self.__input = ""
        self.__G = 𝔈_0
        self.__X = 𝔛_0
        self.__Δt = Δt
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
    def get_parameters(self):
        G = self.__G
        if self.__input == "up":
            self.__G = 𝔈_(G.v_1, G.v_2, G.q_1, G.q_2, G.q_3, G.q_4, G.ω_3 + up_down_step)
            self.__input = ""
        if self.__input == "down":
            self.__G = 𝔈_(G.v_1, G.v_2, G.q_1, G.q_2, G.q_3, G.q_4, G.ω_3 - up_down_step)
            self.__input = ""
        return self.__G
    def get_next_real_time(self):
        self.__X = 𝔛_(self.__X.t + self.__Δt)
        return self.__X
    def get_model(self, X_0, Y_0):
        M = F(self.__Δt, X_0, Y_0)
        return M
    def show(self, X, Y):
        print(f"X = {X}, Y = {Y}, G = {self.__G}")
        self.__chart.append(x = X.t, ys = [Y.ω_1, Y.ω_2, self.__G.ω_3])
H = Helpers(𝔈_0, 𝔛_0, Δt, chart)

# Interactive simulation (regard pseudo-code 4)
G = H.get_parameters()
X = 𝔛_0
Y = 𝔜_0
M = H.get_model(X, Y)
while H.not_terminated():
    t_real = H.get_next_real_time().t
    𝔈 = H.get_parameters()
    if 𝔈 != G:
      M = H.get_model(X, Y)
      G = 𝔈
    X = 𝔛_(t_real)
    Y = simulation(M, [X], G)[0]
    H.show(X, Y)

# Make chart stay shown
chart.show()
